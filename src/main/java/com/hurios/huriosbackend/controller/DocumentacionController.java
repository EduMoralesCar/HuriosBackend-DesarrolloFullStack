package com.hurios.huriosbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentacionController {

    @GetMapping(value = "/documentacion", produces = "text/html;charset=UTF-8")
    @org.springframework.web.bind.annotation.ResponseBody
    public String documentacion() {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>Documentación API</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        html, body { width: 100%; height: 100%; overflow: hidden; }
                        iframe { width: 100%; height: 100%; border: none; }
                    </style>
                </head>
                <body>
                    <iframe src="/swagger-ui/index.html"></iframe>
                </body>
                </html>
                """;
    }
}
