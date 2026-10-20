package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hurios.huriosbackend.dto.PaymentDtos;
import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.entity.SaleItem;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.SaleItemRepository;
import com.hurios.huriosbackend.repository.SaleRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        Sale sale = new Sale();
        sale.setUser(user);

        PaymentDtos.CheckoutInfo info = request.getCheckoutInfo();
        sale.setFullName(info.getFullName());
        sale.setPhone(info.getPhone());
        sale.setDocumentType(info.getDocumentType());
        sale.setDni(info.getDni());
        sale.setCompanyName(info.getCompanyName());
        sale.setRuc(info.getRuc());
        sale.setCompanyAddress(info.getCompanyAddress());
        sale.setDeliveryMethod(info.getDeliveryMethod());
        sale.setDeliveryAddress(info.getDeliveryAddress());
        sale.setDeliveryDistrict(info.getDeliveryDistrict());
        sale.setDeliveryReference(info.getDeliveryReference());
        sale.setPaymentMethod(request.getPaymentMethod());

        try {
            String paymentDetailsJson = objectMapper.writeValueAsString(request.getPaymentDetails());
            sale.setPaymentDetails(paymentDetailsJson);
        } catch (Exception e) {
            sale.setPaymentDetails("{}");
        }

        sale.setSubtotal(request.getTotalPrice());
        sale.setShippingCost(request.getShippingCost());
        sale.setTotal(request.getFinalTotal());
        sale.setStatus("CONFIRMADO");
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());

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

}
