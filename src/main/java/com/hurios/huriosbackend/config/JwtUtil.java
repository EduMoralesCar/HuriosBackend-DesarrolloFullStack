package com.hurios.huriosbackend.config;

// Librerías de jjwt para generar y validar JWT
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/*
 * JwtUtil: utilitario para generar y validar tokens JWT.
 * - usa HS256 y una clave derivada de la propiedad jwt.secret.
 * - expira tokens según jwt.expiration.minutes.
 */
@Component
public class JwtUtil {

    // secret tomado desde application.properties (inyectado)
    @Value("${jwt.secret}")
    private String jwtSecret;

    // tiempo de expiración en minutos
    @Value("${jwt.expiration.minutes}")
    private long jwtExpirationMinutes;

    // Construye la Key para firmar/verificar los JWT
    private Key getSigningKey() {
        // Keys.hmacShaKeyFor requiere un byte[] suficientemente largo
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Genera un token cuyo subject será el identificador (user id o email)
    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMinutes * 60 * 1000);
        return Jwts.builder()
                .setSubject(subject)                        // sub: user id o email
                .setIssuedAt(now)                           // iat
                .setExpiration(exp)                         // exp
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // firmar con HS256
                .compact();
    }

    // Valida un token y devuelve el subject (lanza JwtException si inválido)
    public String validateAndGetSubject(String token) throws JwtException {
        Jws<Claims> parsed = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // clave de verificación
                .build()
                .parseClaimsJws(token);         // valida firma y expiración
        return parsed.getBody().getSubject();
    }
}

