package com.hurios.huriosbackend.dto;

public class AuthRequests {
    // DTO para register
    public static class RegisterRequest {
        public String email;
        public String password;
    }

    // DTO para login
    public static class LoginRequest {
        public String email;
        public String password;
    }
}
