package com.hurios.huriosbackend.dto;

public class AuthResponses {
    public static class LoginResponse {
        public String token;
        public String email;
        public LoginResponse(String token, String email){
            this.token = token;
            this.email = email;
        }
    }
}
