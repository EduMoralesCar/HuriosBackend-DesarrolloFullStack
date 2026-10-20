package com.hurios.huriosbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos.
 * En producción prioriza SendGrid por API HTTP; si no hay API key usa SMTP.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final JavaMailSender mailSender;
    private final ValidationService validationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.from-name:Hurios Rally}")
    private String fromName;

    @Value("${app.email.mock:false}")
    private boolean mockEmail;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${development.email-override:edumoralescarlos@gmail.com}")
    private String emailOverride;


    public EmailService(JavaMailSender mailSender, ValidationService validationService, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendHtml(String to, String subject, String html) throws Exception {
        sendHtml(to, subject, html, false);
    }

    public void sendHtml(String to, String subject, String html, boolean includeLogo) throws Exception {
        if (mockEmail) {
            logger.info("\n{}", "=".repeat(50));
            logger.info("[MOCK EMAIL] To: {}", to);
            logger.info("[MOCK EMAIL] Subject: {}", subject);
            logger.info("[MOCK EMAIL] Content: {}", html);
            logger.info("{}\n", "=".repeat(50));
            return;
        }

        if (hasResendApiKey()) {
            sendViaResend(to, subject, html, includeLogo);
            return;
        }

        sendViaSmtp(to, subject, html, includeLogo);
    }

    private void sendViaResend(String to, String subject, String html, boolean includeLogo) throws Exception {
        try {
            String targetEmail = (emailOverride != null && !emailOverride.isBlank()) ? emailOverride.trim() : to;
            String fromAddress = (from != null && from.contains("@") && !from.endsWith("@gmail.com"))
                    ? buildFromAddress()
                    : "Hurios Rally <onboarding@resend.dev>";

            logger.info("Attempting to send email through Resend to: {} (original: {}) from: {}", targetEmail, to, fromAddress);

            String renderedHtml = includeLogo ? replaceCidLogo(html) : html;
            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", List.of(targetEmail),
                    "subject", subject + " [Para: " + to + "]",
                    "html", renderedHtml
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.error("Resend rejected email with status {}: {}", response.statusCode(), response.body());
                throw new Exception("Resend rechazó el correo: " + response.body());
            }

            logger.info("Email sent successfully through Resend to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email through Resend to {}: {}", to, e.getMessage());
            throw new Exception("Error al enviar email con Resend: " + e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String html, boolean includeLogo) throws Exception {
        try {
            String targetEmail = (emailOverride != null && !emailOverride.isBlank()) ? emailOverride.trim() : to;
            logger.info("Attempting to send email through SMTP to: {} (Original recipient: {})", targetEmail, to);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(targetEmail);
            helper.setSubject(subject + " [Para: " + to + "]");
            helper.setText(html, true);


            if (includeLogo) {
                attachInlineLogo(helper);
            }

            mailSender.send(message);
            logger.info("Email sent successfully through SMTP to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email through SMTP to {}: {}", to, e.getMessage());
            throw new Exception("Error al enviar email: " + e.getMessage(), e);
        }
    }

    private void attachInlineLogo(MimeMessageHelper helper) {
        try {
            File logoFile = new File("../huriosfrontend/public/assets/imgs/logo.webp");
            if (logoFile.exists()) {
                helper.addInline("logo", new FileSystemResource(logoFile));
                logger.info("Inline logo attached for SMTP email");
            } else {
                logger.warn("Logo file not found at: {}", logoFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.warn("Could not attach inline logo: {}", e.getMessage());
        }
    }

    private String replaceCidLogo(String html) {
        return html.replace("<img src='cid:logo' alt='Hurios Rally' />", "")
                .replace("<img src=\"cid:logo\" alt=\"Hurios Rally\" />", "");
    }

    private String buildFromAddress() {
        if (fromName == null || fromName.trim().isEmpty()) {
            return from;
        }
        return fromName.trim() + " <" + from.trim() + ">";
    }

    private boolean hasResendApiKey() {
        return resendApiKey != null && !resendApiKey.trim().isEmpty();
    }

    public void sendPurchaseConfirmation(String to, Long orderId, Double total) throws Exception {
        validationService.validateEmail(to);
        validationService.validateId(orderId);
        validationService.validatePrice(total);

        String subject = "Confirmación de Compra - Orden #" + orderId;
        String html = String.format(
                "<h2>Estimado cliente,</h2>"
                        + "<p>Tu compra ha sido confirmada.</p>"
                        + "<p><strong>Número de orden:</strong> #%d</p>"
                        + "<p><strong>Total:</strong> S/ %.2f</p>"
                        + "<p>Gracias por tu compra.</p>"
                        + "<p>Saludos,<br>Equipo Hurios Rally</p>",
                orderId, total);

        sendHtml(to, subject, html);
    }

    public void sendOrderStatusUpdate(String to, Long orderId, String newStatus) throws Exception {
        validationService.validateEmail(to);
        validationService.validateId(orderId);

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo estado no puede estar vacío");
        }

        String subject = "Actualización de Estado - Orden #" + orderId;
        String html = String.format(
                "<h2>Estimado cliente,</h2>"
                        + "<p>El estado de tu orden #%d ha cambiado.</p>"
                        + "<p><strong>Nuevo estado:</strong> %s</p>"
                        + "<p>Saludos,<br>Equipo Hurios Rally</p>",
                orderId, newStatus);

        sendHtml(to, subject, html);
    }

    public void sendLowStockAlert(String to, String productName, Integer currentStock) throws Exception {
        validationService.validateEmail(to);

        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de producto inválido");
        }

        if (currentStock == null || currentStock < 0) {
            throw new IllegalArgumentException("Stock inválido");
        }

        String subject = "Alerta: Stock Bajo - " + productName;
        String html = String.format(
                "<h2>Alerta de Stock</h2>"
                        + "<p>El producto <strong>'%s'</strong> tiene stock bajo.</p>"
                        + "<p><strong>Stock actual:</strong> %d unidades</p>"
                        + "<p>Se recomienda realizar un nuevo pedido.</p>"
                        + "<p>Sistema Hurios Rally</p>",
                productName, currentStock);

        sendHtml(to, subject, html);
    }

    public void sendOutOfStockAlert(String to, String productName) throws Exception {
        validationService.validateEmail(to);

        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de producto inválido");
        }

        String subject = "Alerta: Producto Sin Stock - " + productName;
        String html = String.format(
                "<h2>Alerta de Stock</h2>"
                        + "<p>El producto <strong>'%s'</strong> se ha quedado sin stock.</p>"
                        + "<p>Se requiere reabastecimiento urgente.</p>"
                        + "<p>Sistema Hurios Rally</p>",
                productName);

        sendHtml(to, subject, html);
    }

    public boolean isValidEmail(String email) {
        try {
            validationService.validateEmail(email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isMockMode() {
        return mockEmail;
    }
}
