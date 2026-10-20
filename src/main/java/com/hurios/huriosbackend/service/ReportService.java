package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.SaleRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService - Servicio para generación de reportes y estadísticas
 * Responsabilidades:
 * - Reportes de ventas
 * - Estadísticas de productos
 * - Análisis de clientes
 * - Reportes de inventario
 * - Métricas del negocio
 */
@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ValidationService validationService;

    public ReportService(SaleRepository saleRepository, 
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        ValidationService validationService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
    }

    // ==================== REPORTES DE VENTAS ====================

    /**
     * Obtener total de ventas en un rango de fechas
     */
    public SalesReport getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        List<Sale> sales = saleRepository.findAll().stream()
                .filter(sale -> sale.getCreatedAt() != null)
                .filter(sale -> !sale.getCreatedAt().isBefore(startDate) && 
                               !sale.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());

        return generateSalesReport(sales, startDate, endDate);
    }

    /**
     * Obtener reporte de ventas del día actual
     */
    public SalesReport getTodaySalesReport() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return getSalesReport(startOfDay, endOfDay);
    }

    /**
     * Obtener reporte de ventas del mes actual
     */
    public SalesReport getCurrentMonthSalesReport() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                                      .atTime(23, 59, 59);
        return getSalesReport(startOfMonth, endOfMonth);
    }

    /**
     * Obtener reporte de ventas del año actual
     */
    public SalesReport getCurrentYearSalesReport() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfYear = now.withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfYear = now.withDayOfYear(now.lengthOfYear())
                                    .atTime(23, 59, 59);
        return getSalesReport(startOfYear, endOfYear);
    }

    /**
     * Generar reporte de ventas a partir de una lista
     */
    private SalesReport generateSalesReport(List<Sale> sales, LocalDateTime start, LocalDateTime end) {
        SalesReport report = new SalesReport();
        report.setStartDate(start);
        report.setEndDate(end);
        report.setTotalSales(sales.size());

        double totalRevenue = sales.stream()
                .filter(sale -> sale.getTotal() != null)
                .mapToDouble(Sale::getTotal)
                .sum();
        report.setTotalRevenue(totalRevenue);

        double averageOrderValue = sales.isEmpty() ? 0 : totalRevenue / sales.size();
        report.setAverageOrderValue(averageOrderValue);

        // Contar ventas por estado
        Map<String, Long> salesByStatus = sales.stream()
                .collect(Collectors.groupingBy(
                    sale -> sale.getStatus() != null ? sale.getStatus() : "DESCONOCIDO",
                    Collectors.counting()
                ));
        report.setSalesByStatus(salesByStatus);

        // Top productos vendidos
        Map<String, Integer> productSales = new HashMap<>();
        sales.forEach(sale -> {
            if (sale.getItems() != null) {
                sale.getItems().forEach(item -> {
                    String productName = item.getProduct() != null ? 
                                        item.getProduct().getName() : "Desconocido";
                    productSales.merge(productName, item.getQuantity(), Integer::sum);
                });
            }
        });
        report.setTopProducts(productSales);

        return report;
    }

    // ==================== REPORTES DE PRODUCTOS ====================

    /**
     * Obtener reporte de inventario
     */
    public InventoryReport getInventoryReport() {
        List<Product> products = productRepository.findAll();
        
        InventoryReport report = new InventoryReport();
        report.setTotalProducts(products.size());

        int totalStock = products.stream()
                .filter(p -> p.getStock() != null)
                .mapToInt(Product::getStock)
                .sum();
        report.setTotalStock(totalStock);

        double totalInventoryValue = products.stream()
                .filter(p -> p.getStock() != null && p.getPrice() != null)
                .mapToDouble(p -> p.getStock() * p.getPrice())
                .sum();
        report.setTotalInventoryValue(totalInventoryValue);

        // Productos sin stock
        long outOfStockCount = products.stream()
                .filter(p -> p.getStock() == null || p.getStock() == 0)
                .count();
        report.setOutOfStockProducts((int) outOfStockCount);

        // Productos con bajo stock (menos de 10)
        long lowStockCount = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() < 10)
                .count();
        report.setLowStockProducts((int) lowStockCount);

        // Productos con buen stock
        long goodStockCount = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() >= 10)
                .count();
        report.setGoodStockProducts((int) goodStockCount);

        return report;
    }

    /**
     * Obtener productos más vendidos
     */
    public List<ProductSalesStats> getTopSellingProducts(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        List<Sale> sales = saleRepository.findAll();
        Map<Long, ProductSalesStats> productStats = new HashMap<>();

        sales.forEach(sale -> {
            if (sale.getItems() != null) {
                sale.getItems().forEach(item -> {
                    if (item.getProduct() != null) {
                        Long productId = item.getProduct().getId();
                        ProductSalesStats stats = productStats.computeIfAbsent(
                            productId, 
                            id -> new ProductSalesStats(
                                productId,
                                item.getProduct().getName(),
                                0,
                                0.0
                            )
                        );
                        stats.addQuantity(item.getQuantity());
                        stats.addRevenue(item.getSubtotal() != null ? item.getSubtotal() : 0);
                    }
                });
            }
        });

        return productStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.getQuantitySold(), a.getQuantitySold()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== REPORTES DE CLIENTES ====================

    /**
     * Obtener reporte de clientes
     */
    public CustomerReport getCustomerReport() {
        List<User> users = userRepository.findAll();
        List<Sale> sales = saleRepository.findAll();

        CustomerReport report = new CustomerReport();
        report.setTotalCustomers(users.size());

        // Clientes activos (con al menos una compra)
        Set<Long> activeCustomerIds = sales.stream()
                .filter(sale -> sale.getUser() != null)
                .map(sale -> sale.getUser().getId())
                .collect(Collectors.toSet());
        report.setActiveCustomers(activeCustomerIds.size());

        // Nuevos clientes este mes
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newCustomersThisMonth = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> !user.getCreatedAt().isBefore(startOfMonth))
                .count();
        report.setNewCustomersThisMonth((int) newCustomersThisMonth);

        return report;
    }

    /**
     * Obtener clientes top (por gasto total)
     */
    public List<CustomerSpendingStats> getTopCustomers(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        List<Sale> sales = saleRepository.findAll();
        Map<Long, CustomerSpendingStats> customerStats = new HashMap<>();

        sales.forEach(sale -> {
            if (sale.getUser() != null && sale.getTotal() != null) {
                Long userId = sale.getUser().getId();
                CustomerSpendingStats stats = customerStats.computeIfAbsent(
                    userId,
                    id -> new CustomerSpendingStats(
                        userId,
                        sale.getUser().getEmail(),
                        0,
                        0.0
                    )
                );
                stats.incrementOrders();
                stats.addSpending(sale.getTotal());
            }
        });

        return customerStats.values().stream()
                .sorted((a, b) -> Double.compare(b.getTotalSpent(), a.getTotalSpent()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== MÉTRICAS DEL NEGOCIO ====================

    /**
     * Obtener dashboard con métricas generales
     */
    public BusinessDashboard getBusinessDashboard() {
        BusinessDashboard dashboard = new BusinessDashboard();

        // Ventas totales
        List<Sale> allSales = saleRepository.findAll();
        dashboard.setTotalSalesCount(allSales.size());
        
        double totalRevenue = allSales.stream()
                .filter(sale -> sale.getTotal() != null)
                .mapToDouble(Sale::getTotal)
                .sum();
        dashboard.setTotalRevenue(totalRevenue);

        // Productos
        dashboard.setTotalProducts((int) productRepository.count());

        // Clientes
        dashboard.setTotalCustomers((int) userRepository.count());

        // Ventas de hoy
        SalesReport todayReport = getTodaySalesReport();
        dashboard.setTodaySales(todayReport.getTotalSales());
        dashboard.setTodayRevenue(todayReport.getTotalRevenue());

        // Ventas del mes
        SalesReport monthReport = getCurrentMonthSalesReport();
        dashboard.setMonthSales(monthReport.getTotalSales());
        dashboard.setMonthRevenue(monthReport.getTotalRevenue());

        // Inventario
        InventoryReport inventoryReport = getInventoryReport();
        dashboard.setOutOfStockProducts(inventoryReport.getOutOfStockProducts());
        dashboard.setLowStockProducts(inventoryReport.getLowStockProducts());

        return dashboard;
    }

    /**
     * Comparar rendimiento entre dos períodos
     */
    public PeriodComparison comparePeriods(LocalDateTime start1, LocalDateTime end1,
                                          LocalDateTime start2, LocalDateTime end2) {
        SalesReport period1 = getSalesReport(start1, end1);
        SalesReport period2 = getSalesReport(start2, end2);

        PeriodComparison comparison = new PeriodComparison();
        comparison.setPeriod1Report(period1);
        comparison.setPeriod2Report(period2);

        // Calcular cambios
        int salesChange = period2.getTotalSales() - period1.getTotalSales();
        comparison.setSalesChange(salesChange);

        double revenueChange = period2.getTotalRevenue() - period1.getTotalRevenue();
        comparison.setRevenueChange(revenueChange);

        double period1Revenue = period1.getTotalRevenue();
        if (period1Revenue > 0) {
            double growthPercentage = ((period2.getTotalRevenue() - period1Revenue) / period1Revenue) * 100;
            comparison.setGrowthPercentage(growthPercentage);
        } else {
            comparison.setGrowthPercentage(0.0);
        }

        return comparison;
    }

    // ==================== CLASES INTERNAS PARA REPORTES ====================

    public static class SalesReport {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int totalSales;
        private double totalRevenue;
        private double averageOrderValue;
        private Map<String, Long> salesByStatus;
        private Map<String, Integer> topProducts;

        // Getters y setters
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public int getTotalSales() { return totalSales; }
        public void setTotalSales(int totalSales) { this.totalSales = totalSales; }
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        public double getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
        public Map<String, Long> getSalesByStatus() { return salesByStatus; }
        public void setSalesByStatus(Map<String, Long> salesByStatus) { this.salesByStatus = salesByStatus; }
        public Map<String, Integer> getTopProducts() { return topProducts; }
        public void setTopProducts(Map<String, Integer> topProducts) { this.topProducts = topProducts; }
    }

    public static class InventoryReport {
        private int totalProducts;
        private int totalStock;
        private double totalInventoryValue;
        private int outOfStockProducts;
        private int lowStockProducts;
        private int goodStockProducts;

        // Getters y setters
        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
        public int getTotalStock() { return totalStock; }
        public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
        public double getTotalInventoryValue() { return totalInventoryValue; }
        public void setTotalInventoryValue(double totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }
        public int getOutOfStockProducts() { return outOfStockProducts; }
        public void setOutOfStockProducts(int outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }
        public int getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(int lowStockProducts) { this.lowStockProducts = lowStockProducts; }
        public int getGoodStockProducts() { return goodStockProducts; }
        public void setGoodStockProducts(int goodStockProducts) { this.goodStockProducts = goodStockProducts; }
    }

    public static class CustomerReport {
        private int totalCustomers;
        private int activeCustomers;
        private int newCustomersThisMonth;

        // Getters y setters
        public int getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
        public int getActiveCustomers() { return activeCustomers; }
        public void setActiveCustomers(int activeCustomers) { this.activeCustomers = activeCustomers; }
        public int getNewCustomersThisMonth() { return newCustomersThisMonth; }
        public void setNewCustomersThisMonth(int newCustomersThisMonth) { this.newCustomersThisMonth = newCustomersThisMonth; }
    }

    public static class ProductSalesStats {
        private Long productId;
        private String productName;
        private int quantitySold;
        private double totalRevenue;

        public ProductSalesStats(Long productId, String productName, int quantitySold, double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.totalRevenue = totalRevenue;
        }

        public void addQuantity(int quantity) {
            this.quantitySold += quantity;
        }

        public void addRevenue(double revenue) {
            this.totalRevenue += revenue;
        }

        // Getters
        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantitySold() { return quantitySold; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    public static class CustomerSpendingStats {
        private Long customerId;
        private String customerEmail;
        private int totalOrders;
        private double totalSpent;

        public CustomerSpendingStats(Long customerId, String customerEmail, int totalOrders, double totalSpent) {
            this.customerId = customerId;
            this.customerEmail = customerEmail;
            this.totalOrders = totalOrders;
            this.totalSpent = totalSpent;
        }

        public void incrementOrders() {
            this.totalOrders++;
        }

        public void addSpending(double amount) {
            this.totalSpent += amount;
        }

        // Getters
        public Long getCustomerId() { return customerId; }
        public String getCustomerEmail() { return customerEmail; }
        public int getTotalOrders() { return totalOrders; }
        public double getTotalSpent() { return totalSpent; }
    }

    public static class BusinessDashboard {
        private int totalSalesCount;
        private double totalRevenue;
        private int totalProducts;
        private int totalCustomers;
        private int todaySales;
        private double todayRevenue;
        private int monthSales;
        private double monthRevenue;
        private int outOfStockProducts;
        private int lowStockProducts;

        // Getters y setters
        public int getTotalSalesCount() { return totalSalesCount; }
        public void setTotalSalesCount(int totalSalesCount) { this.totalSalesCount = totalSalesCount; }
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
        public int getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
        public int getTodaySales() { return todaySales; }
        public void setTodaySales(int todaySales) { this.todaySales = todaySales; }
        public double getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(double todayRevenue) { this.todayRevenue = todayRevenue; }
        public int getMonthSales() { return monthSales; }
        public void setMonthSales(int monthSales) { this.monthSales = monthSales; }
        public double getMonthRevenue() { return monthRevenue; }
        public void setMonthRevenue(double monthRevenue) { this.monthRevenue = monthRevenue; }
        public int getOutOfStockProducts() { return outOfStockProducts; }
        public void setOutOfStockProducts(int outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }
        public int getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(int lowStockProducts) { this.lowStockProducts = lowStockProducts; }
    }

    public static class PeriodComparison {
        private SalesReport period1Report;
        private SalesReport period2Report;
        private int salesChange;
        private double revenueChange;
        private double growthPercentage;

        // Getters y setters
        public SalesReport getPeriod1Report() { return period1Report; }
        public void setPeriod1Report(SalesReport period1Report) { this.period1Report = period1Report; }
        public SalesReport getPeriod2Report() { return period2Report; }
        public void setPeriod2Report(SalesReport period2Report) { this.period2Report = period2Report; }
        public int getSalesChange() { return salesChange; }
        public void setSalesChange(int salesChange) { this.salesChange = salesChange; }
        public double getRevenueChange() { return revenueChange; }
        public void setRevenueChange(double revenueChange) { this.revenueChange = revenueChange; }
        public double getGrowthPercentage() { return growthPercentage; }
        public void setGrowthPercentage(double growthPercentage) { this.growthPercentage = growthPercentage; }
    }
}
