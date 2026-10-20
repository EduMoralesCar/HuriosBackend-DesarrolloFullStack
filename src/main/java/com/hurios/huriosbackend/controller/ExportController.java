package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExportController - Endpoints para exportar reportes en Excel
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    private final ExcelExportService excelExportService;

    public ExportController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    /**
     * GET /export/clients - Exportar clientes a Excel
     */
    @GetMapping("/clients")
    public ResponseEntity<byte[]> exportClients() {
        try {
            byte[] excelData = excelExportService.exportClients();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "Clientes_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
 /**
     * GET /export/products - Exportar productos a Excel
     */
    @GetMapping("/products")
    public ResponseEntity<byte[]> exportProducts() {
        try {
            byte[] excelData = excelExportService.exportProducts();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "Productos_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
/**
     * GET /export/sales - Exportar ventas a Excel
     */
    @GetMapping("/sales")
    public ResponseEntity<byte[]> exportSales() {
        try {
            byte[] excelData = excelExportService.exportSales();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "Ventas_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
}
