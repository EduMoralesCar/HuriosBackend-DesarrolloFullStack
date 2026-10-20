package com.hurios.huriosbackend.dto;

import java.util.List;
import java.util.Map;

public class PaymentDtos {

    public static class ProcessPaymentRequest {
        private CheckoutInfo checkoutInfo;
        private String paymentMethod;
        private Map<String, Object> paymentDetails;
        private List<OrderItem> items;
        private Double totalPrice;
        private Double shippingCost;
        private Double finalTotal;

        public CheckoutInfo getCheckoutInfo() {
            return checkoutInfo;
        }

        public void setCheckoutInfo(CheckoutInfo checkoutInfo) {
            this.checkoutInfo = checkoutInfo;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Map<String, Object> getPaymentDetails() {
            return paymentDetails;
        }

        public void setPaymentDetails(Map<String, Object> paymentDetails) {
            this.paymentDetails = paymentDetails;
        }

        public List<OrderItem> getItems() {
            return items;
        }

        public void setItems(List<OrderItem> items) {
            this.items = items;
        }

        public Double getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(Double totalPrice) {
            this.totalPrice = totalPrice;
        }

        public Double getShippingCost() {
            return shippingCost;
        }

        public void setShippingCost(Double shippingCost) {
            this.shippingCost = shippingCost;
        }

        public Double getFinalTotal() {
            return finalTotal;
        }

        public void setFinalTotal(Double finalTotal) {
            this.finalTotal = finalTotal;
        }
    }

    public static class CheckoutInfo {
        private String fullName;
        private String phone;
        private String documentType;
        private String dni;
        private String companyName;
        private String ruc;
        private String companyAddress;
        private String deliveryMethod;
        private String deliveryAddress;
        private String deliveryReference;
        private String deliveryDistrict;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getDni() {
            return dni;
        }

        public void setDni(String dni) {
            this.dni = dni;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getRuc() {
            return ruc;
        }

        public void setRuc(String ruc) {
            this.ruc = ruc;
        }

        public String getCompanyAddress() {
            return companyAddress;
        }

        public void setCompanyAddress(String companyAddress) {
            this.companyAddress = companyAddress;
        }

        public String getDeliveryMethod() {
            return deliveryMethod;
        }

        public void setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
        }

        public String getDeliveryAddress() {
            return deliveryAddress;
        }

        public void setDeliveryAddress(String deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
        }

        public String getDeliveryReference() {
            return deliveryReference;
        }

        public void setDeliveryReference(String deliveryReference) {
            this.deliveryReference = deliveryReference;
        }

        public String getDeliveryDistrict() {
            return deliveryDistrict;
        }

        public void setDeliveryDistrict(String deliveryDistrict) {
            this.deliveryDistrict = deliveryDistrict;
        }
    }

    public static class OrderItem {
        private Long productId;
        private Integer quantity;
        private Double price;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    public static class ProcessPaymentResponse {
        private boolean success;
        private String message;
        private Long orderId;
        private String orderNumber;

        public ProcessPaymentResponse(boolean success, String message, Long orderId) {
            this.success = success;
            this.message = message;
            this.orderId = orderId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    public static class CheckoutPreferenceResponse {
        private boolean success;
        private Long saleId;
        private String preferenceId;
        private String initPoint;
        private String sandboxInitPoint;
        private String transactionId;
        private String message;

        public CheckoutPreferenceResponse() {
        }

        public CheckoutPreferenceResponse(boolean success, Long saleId, String preferenceId,
                                          String initPoint, String sandboxInitPoint,
                                          String transactionId, String message) {
            this.success = success;
            this.saleId = saleId;
            this.preferenceId = preferenceId;
            this.initPoint = initPoint;
            this.sandboxInitPoint = sandboxInitPoint;
            this.transactionId = transactionId;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getSaleId() {
            return saleId;
        }

        public void setSaleId(Long saleId) {
            this.saleId = saleId;
        }

        public String getPreferenceId() {
            return preferenceId;
        }

        public void setPreferenceId(String preferenceId) {
            this.preferenceId = preferenceId;
        }

        public String getInitPoint() {
            return initPoint;
        }

        public void setInitPoint(String initPoint) {
            this.initPoint = initPoint;
        }

        public String getSandboxInitPoint() {
            return sandboxInitPoint;
        }

        public void setSandboxInitPoint(String sandboxInitPoint) {
            this.sandboxInitPoint = sandboxInitPoint;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class PaymentVerificationResponse {
        private boolean approved;
        private Long saleId;
        private String status;
        private String mpPaymentId;
        private String paymentMethod;
        private Double total;
        private String transactionId;
        private String message;

        public PaymentVerificationResponse() {
        }

        public PaymentVerificationResponse(boolean approved, Long saleId, String status,
                                           String mpPaymentId, String paymentMethod,
                                           Double total, String transactionId, String message) {
            this.approved = approved;
            this.saleId = saleId;
            this.status = status;
            this.mpPaymentId = mpPaymentId;
            this.paymentMethod = paymentMethod;
            this.total = total;
            this.transactionId = transactionId;
            this.message = message;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public Long getSaleId() {
            return saleId;
        }

        public void setSaleId(Long saleId) {
            this.saleId = saleId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMpPaymentId() {
            return mpPaymentId;
        }

        public void setMpPaymentId(String mpPaymentId) {
            this.mpPaymentId = mpPaymentId;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
