package com.hurios.huriosbackend.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicStatusController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "message", "Backend Hurios Rally activo",
                "status", "ok"
        ));
    }

    @GetMapping("/health-public")
    public ResponseEntity<Map<String, String>> healthPublic() {
        return ResponseEntity.ok(Map.of(
                "message", "Backend Hurios Rally activo",
                "status", "ok"
        ));
    }
}
