package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.document.PaymentTransactionDocument;
import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.entity.SaleItem;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.SaleItemRepository;
import com.hurios.huriosbackend.repository.SaleRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.mongo.PaymentTransactionRepository;
import com.hurios.huriosbackend.websocket.RealtimeNotificationHandler;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @Autowired(required = false)
    private RealtimeNotificationHandler realtimeNotificationHandler;

    @Value("${mercadopago.access-token:}")
    private String mpAccessToken;

    @Value("${mercadopago.webhook-url:}")
    private String mpWebhookUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Crea una venta en estado PENDIENTE en PostgreSQL, genera la preferencia de pago en MercadoPago
     * (Checkout Pro) y registra la transacción inicial en MongoDB ("payment_transactions").
     * El stock NO se descuenta hasta que el pago sea aprobado.
     */
    @Transactional
    public PaymentDtos.CheckoutPreferenceResponse createCheckoutPreference(
            PaymentDtos.ProcessPaymentRequest request,
            String userEmail
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("El carrito de compras está vacío");
        }

        // 1. Validar stock disponible sin descontarlo todavía
        for (PaymentDtos.OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductId()));

            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + product.getName());
            }
        }

        // 2. Crear la orden (Sale) en PostgreSQL con estado PENDIENTE
        Sale sale = buildSaleFromRequest(request, user, "PENDIENTE");
        sale.setPaymentMethod("mercadopago");
        sale.setPaymentDetails("Transacción almacenada en MongoDB (payment_transactions)");
        sale = saleRepository.save(sale);

        List<PreferenceItemRequest> mpItems = new ArrayList<>();

        for (PaymentDtos.OrderItem orderItem : request.getItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(orderItem.getQuantity());
            saleItem.setUnitPrice(orderItem.getPrice());
            saleItem.setSubtotal(orderItem.getQuantity() * orderItem.getPrice());
            sale.addItem(saleItem);

            mpItems.add(PreferenceItemRequest.builder()
                    .id(String.valueOf(product.getId()))
                    .title(product.getName())
                    .quantity(orderItem.getQuantity())
                    .currencyId("PEN")
                    .unitPrice(BigDecimal.valueOf(orderItem.getPrice()))
                    .build());
        }

        if (request.getShippingCost() != null && request.getShippingCost() > 0) {
            mpItems.add(PreferenceItemRequest.builder()
                    .id("SHIPPING")
                    .title("Costo de envío - Hurios Rally")
                    .quantity(1)
                    .currencyId("PEN")
                    .unitPrice(BigDecimal.valueOf(request.getShippingCost()))
                    .build());
        }

        sale = saleRepository.save(sale);

        // 3. Crear preferencia en MercadoPago SDK (o URL sandbox de respaldo si no hay token configurado)
        String baseFront = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.trim() : "http://localhost:5173";
        String preferenceId;
        String initPoint;
        String sandboxInitPoint;
        Map<String, Object> gatewayData = new HashMap<>();

        if (mpAccessToken != null && !mpAccessToken.isBlank()) {
            try {
                MercadoPagoConfig.setAccessToken(mpAccessToken.trim());
                PreferenceClient client = new PreferenceClient();

                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success(baseFront + "/pago/exito")
                        .failure(baseFront + "/pago/fallo")
                        .pending(baseFront + "/pago/pendiente")
                        .build();

                PreferencePayerRequest payer = PreferencePayerRequest.builder()
                        .name(sale.getFullName())
                        .email(user.getEmail())
                        .build();

                PreferenceRequest.PreferenceRequestBuilder prefBuilder = PreferenceRequest.builder()
                        .items(mpItems)
                        .payer(payer)
                        .backUrls(backUrls)
                        .externalReference(String.valueOf(sale.getId()));

                if (baseFront.startsWith("https://")) {
                    prefBuilder.autoReturn("approved");
                }

                if (mpWebhookUrl != null && !mpWebhookUrl.isBlank()) {
                    prefBuilder.notificationUrl(mpWebhookUrl.trim());
                }

                Preference preference = client.create(prefBuilder.build());
                preferenceId = preference.getId();
                initPoint = preference.getInitPoint();
                sandboxInitPoint = preference.getSandboxInitPoint() != null
                        ? preference.getSandboxInitPoint()
                        : preference.getInitPoint();

                gatewayData.put("preferenceId", preferenceId);
                gatewayData.put("initPoint", initPoint);
                gatewayData.put("sandboxInitPoint", sandboxInitPoint);
                gatewayData.put("externalReference", String.valueOf(sale.getId()));
                gatewayData.put("mode", "MERCADOPAGO_API");
            } catch (Exception e) {
                throw new RuntimeException("Error al crear preferencia en MercadoPago: " + e.getMessage(), e);
            }
        } else {
            preferenceId = "SANDBOX-PREF-" + sale.getId();
            initPoint = baseFront + "/pago/exito?external_reference=" + sale.getId()
                    + "&status=approved&payment_id=SANDBOX-PAY-" + sale.getId()
                    + "&preference_id=" + preferenceId;
            sandboxInitPoint = initPoint;
            gatewayData.put("preferenceId", preferenceId);
            gatewayData.put("initPoint", initPoint);
            gatewayData.put("externalReference", String.valueOf(sale.getId()));
            gatewayData.put("mode", "LOCAL_SANDBOX_SIMULATION");
        }

        // 4. Guardar documento PaymentTransactionDocument en MongoDB ("payment_transactions")
        String transactionId = null;
        if (paymentTransactionRepository != null) {
            PaymentTransactionDocument tx = new PaymentTransactionDocument(
                    sale.getId(),
                    user.getId(),
                    user.getEmail(),
                    sale.getTotal(),
                    "mercadopago"
            );
            tx.setPreferenceId(preferenceId);
            tx.setGatewayResponse(gatewayData);
            tx.addAttempt("PREFERENCE_CREATED", null, "Preferencia de pago generada para la orden #" + sale.getId());
            tx = paymentTransactionRepository.save(tx);
            transactionId = tx.getId();

            sale.setPaymentDetails("MongoDB Tx: " + transactionId);
            saleRepository.save(sale);
        }

        return new PaymentDtos.CheckoutPreferenceResponse(
                true,
                sale.getId(),
                preferenceId,
                initPoint,
                sandboxInitPoint,
                transactionId,
                "Preferencia de pago creada exitosamente"
        );
    }

    /**
     * Verifica el estado del pago al retornar de MercadoPago o al recibir un webhook.
     * Si el pago fue aprobado y la venta seguía PENDIENTE, la marca como CONFIRMADO y descuenta el stock.
     */
    @Transactional
    public PaymentDtos.PaymentVerificationResponse verifyPayment(
            String paymentId,
            String statusParam,
            String externalReference,
            String preferenceId
    ) {
        Long saleId = null;
        if (externalReference != null && !externalReference.isBlank() && !"null".equalsIgnoreCase(externalReference)) {
            try {
                saleId = Long.parseLong(externalReference.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String resolvedStatus = statusParam != null ? statusParam.toLowerCase() : "pending";
        String resolvedPaymentMethod = "mercadopago";
        Map<String, Object> mpGatewayDetails = new HashMap<>();

        // Si tenemos token de MercadoPago y un payment_id numérico real, consultamos el SDK oficial
        if (paymentId != null && paymentId.matches("\\d+") && mpAccessToken != null && !mpAccessToken.isBlank()) {
            try {
                MercadoPagoConfig.setAccessToken(mpAccessToken.trim());
                PaymentClient paymentClient = new PaymentClient();
                Payment mpPayment = paymentClient.get(Long.parseLong(paymentId));
                if (mpPayment != null) {
                    if (mpPayment.getStatus() != null) {
                        resolvedStatus = mpPayment.getStatus().toLowerCase();
                    }
                    if (mpPayment.getPaymentMethodId() != null) {
                        resolvedPaymentMethod = mpPayment.getPaymentMethodId();
                    }
                    if (saleId == null && mpPayment.getExternalReference() != null) {
                        try {
                            saleId = Long.parseLong(mpPayment.getExternalReference());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    mpGatewayDetails.put("mpPaymentId", mpPayment.getId());
                    mpGatewayDetails.put("status", mpPayment.getStatus());
                    mpGatewayDetails.put("statusDetail", mpPayment.getStatusDetail());
                    mpGatewayDetails.put("paymentMethodId", mpPayment.getPaymentMethodId());
                    mpGatewayDetails.put("paymentTypeId", mpPayment.getPaymentTypeId());
                    mpGatewayDetails.put("transactionAmount", mpPayment.getTransactionAmount());
                    mpGatewayDetails.put("currencyId", mpPayment.getCurrencyId());
                    mpGatewayDetails.put("dateApproved", mpPayment.getDateApproved() != null ? mpPayment.getDateApproved().toString() : null);
                }
            } catch (Exception e) {
                mpGatewayDetails.put("verificationWarning", e.getMessage());
            }
        } else {
            mpGatewayDetails.put("paymentId", paymentId);
            mpGatewayDetails.put("statusParam", resolvedStatus);
            mpGatewayDetails.put("preferenceId", preferenceId);
        }

        // Si aún no tenemos saleId pero tenemos preferenceId, buscar en MongoDB
        if (saleId == null && preferenceId != null && paymentTransactionRepository != null) {
            Optional<PaymentTransactionDocument> byPref = paymentTransactionRepository.findByPreferenceId(preferenceId);
            if (byPref.isPresent()) {
                saleId = byPref.get().getSaleId();
            }
        }

        if (saleId == null) {
            throw new RuntimeException("No se pudo identificar la orden asociada al pago");
        }

        final Long targetSaleId = saleId;
        Sale sale = saleRepository.findById(targetSaleId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: #" + targetSaleId));

        boolean approved = "approved".equalsIgnoreCase(resolvedStatus);
        String mongoStatus = approved ? "APPROVED"
                : ("rejected".equalsIgnoreCase(resolvedStatus) || "cancelled".equalsIgnoreCase(resolvedStatus))
                ? "REJECTED" : "PENDING";

        // Actualizar o crear documento en MongoDB ("payment_transactions")
        String transactionId = null;
        if (paymentTransactionRepository != null) {
            final Long finalSaleId = saleId;
            PaymentTransactionDocument tx = paymentTransactionRepository.findBySaleId(finalSaleId)
                    .orElseGet(() -> new PaymentTransactionDocument(
                            finalSaleId,
                            sale.getUser() != null ? sale.getUser().getId() : null,
                            sale.getUser() != null ? sale.getUser().getEmail() : null,
                            sale.getTotal(),
                            "mercadopago"
                    ));

            tx.setStatus(mongoStatus);
            if (paymentId != null && !paymentId.isBlank() && !"null".equalsIgnoreCase(paymentId)) {
                tx.setMpPaymentId(paymentId);
            }
            if (preferenceId != null && !preferenceId.isBlank() && !"null".equalsIgnoreCase(preferenceId)) {
                tx.setPreferenceId(preferenceId);
            }
            tx.setPaymentMethod(resolvedPaymentMethod);
            tx.getGatewayResponse().putAll(mpGatewayDetails);
            tx.addAttempt(mongoStatus, paymentId, "Verificación de pago: " + resolvedStatus);
            tx = paymentTransactionRepository.save(tx);
            transactionId = tx.getId();
        }

        // Actualizar Sale y descontar stock SOLO si el pago fue aprobado y la venta aún no estaba CONFIRMADO
        if (approved && !"CONFIRMADO".equalsIgnoreCase(sale.getStatus())) {
            for (SaleItem saleItem : sale.getItems()) {
                Product product = saleItem.getProduct();
                if (product != null) {
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    int newStock = Math.max(0, currentStock - saleItem.getQuantity());
                    product.setStock(newStock);
                    productRepository.save(product);
                }
            }
            sale.setStatus("CONFIRMADO");
            sale.setPaymentMethod(resolvedPaymentMethod);
            if (transactionId != null) {
                sale.setPaymentDetails("MongoDB Tx: " + transactionId + (paymentId != null ? " | MP: " + paymentId : ""));
            }
            sale.setUpdatedAt(LocalDateTime.now());
            saleRepository.save(sale);

            if (auditService != null && sale.getUser() != null) {
                auditService.logPurchase(sale.getId(), sale.getUser().getId(), sale.getUser().getEmail(),
                        sale.getTotal(), resolvedPaymentMethod, null);
            }

            if (realtimeNotificationHandler != null) {
                realtimeNotificationHandler.broadcastEvent(
                        "NEW_SALE",
                        "Nueva compra confirmada (#" + sale.getId() + ") — Stock actualizado en tiempo real",
                        Map.of(
                                "saleId", sale.getId(),
                                "total", sale.getTotal(),
                                "paymentMethod", resolvedPaymentMethod
                        )
                );
            }
        } else if ("REJECTED".equals(mongoStatus) && !"CONFIRMADO".equalsIgnoreCase(sale.getStatus())) {
            sale.setStatus("RECHAZADO");
            sale.setUpdatedAt(LocalDateTime.now());
            saleRepository.save(sale);
        }

        String msg = approved
                ? "Pago aprobado y orden confirmada exitosamente"
                : ("REJECTED".equals(mongoStatus) ? "El pago fue rechazado" : "El pago se encuentra pendiente de confirmación");

        return new PaymentDtos.PaymentVerificationResponse(
                approved,
                sale.getId(),
                sale.getStatus(),
                paymentId,
                sale.getPaymentMethod(),
                sale.getTotal(),
                transactionId,
                msg
        );
    }

    /**
     * Procesa notificaciones automáticas (Webhooks / IPN) enviadas por MercadoPago.
     */
    @Transactional
    public Map<String, Object> handleWebhook(Map<String, Object> payload, Map<String, String> queryParams) {
        String paymentId = null;

        if (queryParams != null) {
            if (queryParams.containsKey("data.id")) {
                paymentId = queryParams.get("data.id");
            } else if ("payment".equalsIgnoreCase(queryParams.get("topic")) && queryParams.containsKey("id")) {
                paymentId = queryParams.get("id");
            }
        }

        if (paymentId == null && payload != null) {
            Object dataObj = payload.get("data");
            if (dataObj instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                paymentId = String.valueOf(dataMap.get("id"));
            }
        }

        if (paymentId != null && !paymentId.isBlank()) {
            try {
                PaymentDtos.PaymentVerificationResponse verified = verifyPayment(paymentId, null, null, null);
                return Map.of("received", true, "saleId", verified.getSaleId(), "status", verified.getStatus());
            } catch (Exception e) {
                return Map.of("received", true, "note", e.getMessage());
            }
        }

        return Map.of("received", true);
    }

    @Transactional
    public PaymentDtos.ProcessPaymentResponse processPayment(
            PaymentDtos.ProcessPaymentRequest request,
            String userEmail
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        for (PaymentDtos.OrderItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductId()));

            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + product.getName());
            }
        }

        Sale sale = buildSaleFromRequest(request, user, "CONFIRMADO");

        try {
            String paymentDetailsJson = objectMapper.writeValueAsString(request.getPaymentDetails());
            sale.setPaymentDetails(paymentDetailsJson);
        } catch (Exception e) {
            sale.setPaymentDetails("{}");
        }

        sale = saleRepository.save(sale);

        for (PaymentDtos.OrderItem orderItem : request.getItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(orderItem.getQuantity());
            saleItem.setUnitPrice(orderItem.getPrice());
            saleItem.setSubtotal(orderItem.getQuantity() * orderItem.getPrice());

            sale.addItem(saleItem);

            int newStock = product.getStock() - orderItem.getQuantity();
            product.setStock(newStock);
            productRepository.save(product);
        }

        sale = saleRepository.save(sale);

        if (paymentTransactionRepository != null) {
            PaymentTransactionDocument tx = new PaymentTransactionDocument(
                    sale.getId(),
                    user.getId(),
                    user.getEmail(),
                    sale.getTotal(),
                    request.getPaymentMethod()
            );
            tx.setStatus("APPROVED");
            if (request.getPaymentDetails() != null) {
                tx.setGatewayResponse(new HashMap<>(request.getPaymentDetails()));
            }
            tx.addAttempt("APPROVED", "DIRECT-" + sale.getId(), "Pago directo confirmado");
            paymentTransactionRepository.save(tx);
        }

        if (realtimeNotificationHandler != null) {
            realtimeNotificationHandler.broadcastEvent(
                    "NEW_SALE",
                    "Nueva compra procesada (#" + sale.getId() + ") — Stock actualizado en tiempo real",
                    Map.of(
                            "saleId", sale.getId(),
                            "total", sale.getTotal(),
                            "paymentMethod", request.getPaymentMethod() != null ? request.getPaymentMethod() : "tarjeta"
                    )
            );
        }

        return new PaymentDtos.ProcessPaymentResponse(
                true,
                "Pago procesado exitosamente",
                sale.getId()
        );
    }

    public List<Sale> getUserSales(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return saleRepository.findByUserId(user.getId());
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: #" + id));
    }

    public List<PaymentTransactionDocument> getAllTransactions() {
        if (paymentTransactionRepository == null) {
            return List.of();
        }
        return paymentTransactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PaymentTransactionDocument> getTransactionBySaleId(Long saleId) {
        if (paymentTransactionRepository == null) {
            return Optional.empty();
        }
        return paymentTransactionRepository.findBySaleId(saleId);
    }

    private Sale buildSaleFromRequest(PaymentDtos.ProcessPaymentRequest request, User user, String status) {
        Sale sale = new Sale();
        sale.setUser(user);

        PaymentDtos.CheckoutInfo info = request.getCheckoutInfo();
        if (info != null) {
            sale.setFullName(info.getFullName() != null ? info.getFullName() : user.getFullName());
            sale.setPhone(info.getPhone() != null ? info.getPhone() : (user.getPhone() != null ? user.getPhone() : ""));
            sale.setDocumentType(info.getDocumentType() != null ? info.getDocumentType() : "dni");
            sale.setDni(info.getDni());
            sale.setCompanyName(info.getCompanyName());
            sale.setRuc(info.getRuc());
            sale.setCompanyAddress(info.getCompanyAddress());
            sale.setDeliveryMethod(info.getDeliveryMethod() != null ? info.getDeliveryMethod() : "delivery");
            sale.setDeliveryAddress(info.getDeliveryAddress());
            sale.setDeliveryDistrict(info.getDeliveryDistrict());
            sale.setDeliveryReference(info.getDeliveryReference());
        } else {
            sale.setFullName(user.getFullName() != null ? user.getFullName() : user.getEmail());
            sale.setPhone(user.getPhone() != null ? user.getPhone() : "");
            sale.setDocumentType("dni");
            sale.setDeliveryMethod("delivery");
        }

        sale.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "mercadopago");
        sale.setSubtotal(request.getTotalPrice() != null ? request.getTotalPrice() : 0.0);
        sale.setShippingCost(request.getShippingCost() != null ? request.getShippingCost() : 0.0);
        sale.setTotal(request.getFinalTotal() != null ? request.getFinalTotal() : 0.0);
        sale.setStatus(status);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        return sale;
    }
}
