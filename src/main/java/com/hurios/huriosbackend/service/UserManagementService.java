package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.UserRepository;
import com.hurios.huriosbackend.repository.SaleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserManagementService - Servicio completo para gestión de usuarios
 * Responsabilidades:
 * - CRUD de usuarios
 * - Gestión de roles y permisos
 * - Activación/Desactivación de cuentas
 * - Búsqueda y filtrado avanzado
 * - Estadísticas de usuarios
 * - Validación y seguridad
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ValidationService validationService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                SaleRepository saleRepository,
                                ValidationService validationService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.validationService = validationService;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== OPERACIONES CRUD ====================

    /**
     * Obtener todos los usuarios
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtener usuario por ID
     */
    public Optional<User> getUserById(Long id) {
        validationService.validateId(id);
        return userRepository.findById(id);
    }

    /**
     * Obtener usuario por email
     */
    public Optional<User> getUserByEmail(String email) {
        validationService.validateEmail(email);
        String normalizedEmail = validationService.normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Crear nuevo usuario
     */
    @Transactional
    public User createUser(String email, String password, String fullName, String role) {
        // Validaciones
        validationService.validateEmail(email);
        validationService.validatePassword(password);
        
        String normalizedEmail = validationService.normalizeEmail(email);
        
        // Verificar que el email no exista
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + normalizedEmail);
        }
        
        // Validar rol
        String validRole = validateAndNormalizeRole(role);
        
        // Crear usuario
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName != null ? validationService.sanitizeString(fullName) : null);
        user.setRole(validRole);
        user.setVerified(true); // Por defecto verificado
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Actualizar información de usuario
     */
    @Transactional
    public User updateUser(Long id, String fullName, String phone, String address) {
        validationService.validateId(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        
        // Actualizar campos
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(validationService.sanitizeString(fullName));
        }
        
        if (phone != null && !phone.trim().isEmpty()) {
            validationService.validatePhone(phone);
            user.setPhone(phone.trim());
        }
        
        if (address != null) {
            user.setAddress(address.trim().isEmpty() ? null : validationService.sanitizeString(address));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Cambiar contraseña de usuario
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        validationService.validateId(userId);
        validationService.validatePassword(newPassword);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar contraseña anterior
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        
        // Actualizar contraseña
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Restablecer contraseña (admin)
     */
    @Transactional
    public String resetPassword(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Generar contraseña temporal
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        return tempPassword; // Se debe enviar al usuario por email
    }

    /**
     * Eliminar usuario
     */
    @Transactional
    public void deleteUser(Long id) {
        validationService.validateId(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar si tiene compras
        long purchaseCount = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null && sale.getUser().getId().equals(id))
                .count();
        
        if (purchaseCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el usuario porque tiene " + purchaseCount + " compras registradas"
            );
        }
        
        userRepository.deleteById(id);
    }

    // ==================== GESTIÓN DE ROLES ====================

    /**
     * Cambiar rol de usuario
     */
    @Transactional
    public User changeUserRole(Long userId, String newRole) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String validRole = validateAndNormalizeRole(newRole);
        user.setRole(validRole);
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Validar y normalizar rol
     */
    private String validateAndNormalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "CLIENTE"; // Rol por defecto
        }
        
        String upperRole = role.trim().toUpperCase();
        
        if (!upperRole.equals("CLIENTE") && !upperRole.equals("ADMINISTRADOR")) {
            throw new IllegalArgumentException(
                "Rol inválido. Los roles permitidos son: CLIENTE, ADMINISTRADOR"
            );
        }
        
        return upperRole;
    }

    /**
     * Obtener usuarios por rol
     */
    public List<User> getUsersByRole(String role) {
        String validRole = validateAndNormalizeRole(role);
        
        return userRepository.findAll().stream()
                .filter(user -> validRole.equals(user.getRole()))
                .collect(Collectors.toList());
    }

    /**
     * Contar usuarios por rol
     */
    public Map<String, Long> countUsersByRole() {
        List<User> allUsers = userRepository.findAll();
        
        return allUsers.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getRole() != null ? user.getRole() : "SIN_ROL",
                    Collectors.counting()
                ));
    }

    // ==================== VERIFICACIÓN Y ACTIVACIÓN ====================

    /**
     * Verificar cuenta de usuario
     */
    @Transactional
    public void verifyUser(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Desactivar cuenta de usuario
     */
    @Transactional
    public void deactivateUser(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setVerified(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * Verificar si un usuario está activo
     */
    public boolean isUserActive(Long userId) {
        validationService.validateId(userId);
        
        return userRepository.findById(userId)
                .map(User::isVerified)
                .orElse(false);
    }

    // ==================== BÚSQUEDA Y FILTRADO ====================

    /**
     * Buscar usuarios por término de búsqueda
     */
    public List<User> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUsers();
        }
        
        String lowerSearch = searchTerm.toLowerCase().trim();
        
        return userRepository.findAll().stream()
                .filter(user -> 
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerSearch)) ||
                    (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerSearch)) ||
                    (user.getPhone() != null && user.getPhone().contains(lowerSearch))
                )
                .collect(Collectors.toList());
    }

    /**
     * Filtrar usuarios por rango de fechas de registro
     */
    public List<User> getUsersRegisteredBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> !user.getCreatedAt().isBefore(startDate) && 
                               !user.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuarios nuevos (registrados en los últimos N días)
     */
    public List<User> getRecentUsers(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("El número de días debe ser mayor a 0");
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> user.getCreatedAt().isAfter(cutoffDate))
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuarios inactivos (sin compras)
     */
    public List<User> getInactiveUsers() {
        List<Long> activeUserIds = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null)
                .map(sale -> sale.getUser().getId())
                .distinct()
                .collect(Collectors.toList());
        
        return userRepository.findAll().stream()
                .filter(user -> !activeUserIds.contains(user.getId()))
                .collect(Collectors.toList());
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtener estadísticas generales de usuarios
     */
    public UserStatistics getUserStatistics() {
        List<User> allUsers = userRepository.findAll();
        
        UserStatistics stats = new UserStatistics();
        stats.setTotalUsers(allUsers.size());
        
        // Usuarios verificados vs no verificados
        long verifiedCount = allUsers.stream()
                .filter(User::isVerified)
                .count();
        stats.setVerifiedUsers((int) verifiedCount);
        stats.setUnverifiedUsers(allUsers.size() - (int) verifiedCount);
        
        // Usuarios por rol
        stats.setUsersByRole(countUsersByRole());
        
        // Usuarios nuevos este mes
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        long newThisMonth = allUsers.stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> user.getCreatedAt().isAfter(startOfMonth))
                .count();
        stats.setNewUsersThisMonth((int) newThisMonth);
        
        // Usuarios activos (con compras)
        Set<Long> activeUserIds = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null)
                .map(sale -> sale.getUser().getId())
                .collect(Collectors.toSet());
        stats.setActiveUsers(activeUserIds.size());
        stats.setInactiveUsers(allUsers.size() - activeUserIds.size());
        
        return stats;
    }

    /**
     * Obtener perfil completo de usuario con estadísticas
     */
    public UserProfile getUserProfile(Long userId) {
        validationService.validateId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        
        // Estadísticas de compras
        List<Double> purchases = saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null && sale.getUser().getId().equals(userId))
                .filter(sale -> sale.getTotal() != null)
                .map(sale -> sale.getTotal())
                .collect(Collectors.toList());
        
        profile.setTotalPurchases(purchases.size());
        
        double totalSpent = purchases.stream().mapToDouble(Double::doubleValue).sum();
        profile.setTotalSpent(totalSpent);
        
        double averageSpent = purchases.isEmpty() ? 0 : totalSpent / purchases.size();
        profile.setAverageOrderValue(averageSpent);
        
        // Última compra
        saleRepository.findAll().stream()
                .filter(sale -> sale.getUser() != null && sale.getUser().getId().equals(userId))
                .filter(sale -> sale.getCreatedAt() != null)
                .max((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                .ifPresent(sale -> profile.setLastPurchaseDate(sale.getCreatedAt()));
        
        return profile;
    }

    // ==================== UTILIDADES ====================

    /**
     * Verificar si un email ya está registrado
     */
    public boolean emailExists(String email) {
        try {
            validationService.validateEmail(email);
            String normalizedEmail = validationService.normalizeEmail(email);
            return userRepository.findByEmail(normalizedEmail).isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Contar usuarios totales
     */
    public long countTotalUsers() {
        return userRepository.count();
    }

    /**
     * Generar contraseña temporal aleatoria
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    /**
     * Exportar usuarios a formato CSV (String)
     */
    public String exportUsersToCSV() {
        List<User> users = userRepository.findAll();
        StringBuilder csv = new StringBuilder();
        
        // Encabezados
        csv.append("ID,Email,Nombre Completo,Rol,Verificado,Fecha de Registro\n");
        
        // Datos
        users.forEach(user -> {
            csv.append(user.getId()).append(",")
               .append(user.getEmail() != null ? user.getEmail() : "").append(",")
               .append(user.getFullName() != null ? user.getFullName() : "").append(",")
               .append(user.getRole() != null ? user.getRole() : "").append(",")
               .append(user.isVerified() ? "Sí" : "No").append(",")
               .append(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
               .append("\n");
        });
        
        return csv.toString();
    }

    // ==================== CLASES INTERNAS ====================

    public static class UserStatistics {
        private int totalUsers;
        private int verifiedUsers;
        private int unverifiedUsers;
        private int activeUsers;
        private int inactiveUsers;
        private int newUsersThisMonth;
        private Map<String, Long> usersByRole;

        // Getters y setters
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public int getVerifiedUsers() { return verifiedUsers; }
        public void setVerifiedUsers(int verifiedUsers) { this.verifiedUsers = verifiedUsers; }
        public int getUnverifiedUsers() { return unverifiedUsers; }
        public void setUnverifiedUsers(int unverifiedUsers) { this.unverifiedUsers = unverifiedUsers; }
        public int getActiveUsers() { return activeUsers; }
        public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
        public int getInactiveUsers() { return inactiveUsers; }
        public void setInactiveUsers(int inactiveUsers) { this.inactiveUsers = inactiveUsers; }
        public int getNewUsersThisMonth() { return newUsersThisMonth; }
        public void setNewUsersThisMonth(int newUsersThisMonth) { this.newUsersThisMonth = newUsersThisMonth; }
        public Map<String, Long> getUsersByRole() { return usersByRole; }
        public void setUsersByRole(Map<String, Long> usersByRole) { this.usersByRole = usersByRole; }
    }

    public static class UserProfile {
        private User user;
        private int totalPurchases;
        private double totalSpent;
        private double averageOrderValue;
        private LocalDateTime lastPurchaseDate;

        // Getters y setters
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public int getTotalPurchases() { return totalPurchases; }
        public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }
        public double getTotalSpent() { return totalSpent; }
        public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
        public double getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
        public LocalDateTime getLastPurchaseDate() { return lastPurchaseDate; }
        public void setLastPurchaseDate(LocalDateTime lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }
    }
}
