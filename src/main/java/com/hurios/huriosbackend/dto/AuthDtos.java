package com.hurios.huriosbackend.dto;

// DTOs para request bodies simples enviados por el frontend
public class AuthDtos {

    public static class RegisterDto {
        public String email;
        public String password;
        public String name; // opcional
    }

    public static class LoginDto {
        public String email;
        public String password;
    }

    public static class EmailDto {
        public String email;
    }

    public static class VerifyCodeDto {
        public String email;
        public String code;
    }

    public static class ResetRequestDto {
        public String email;
    }

    public static class ResetDto {
        public String email;
        public String token;
        public String newPassword;
    }
}
