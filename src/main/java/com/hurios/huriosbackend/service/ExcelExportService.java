package com.hurios.huriosbackend.service;


import com.hurios.huriosbackend.entity.Sale;
import com.hurios.huriosbackend.entity.SaleItem;
import com.hurios.huriosbackend.repository.SaleRepository;
import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ExcelExportService - Servicio para exportar datos a Excel usando Apache POI
 */
@Service
public class ExcelExportService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    public ExcelExportService(
            UserRepository userRepository,
            ProductRepository productRepository,
            SaleRepository saleRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
    }

    /**
     * Exportar clientes a Excel
     */
    public byte[] exportClients() throws IOException {
        // Obtener solo usuarios con rol CLIENTE
        List<User> clients = userRepository.findAll().stream()
                .filter(user -> "CLIENTE".equals(user.getRole()))
                .toList();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clientes");

            // Agregar logo
            addLogo((XSSFWorkbook) workbook, sheet);

            // Estilo para el header
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Crear header (empezar en fila 4 para dejar espacio al logo)
            Row headerRow = sheet.createRow(4);
            String[] columns = {"ID", "Nombre Completo", "Email", "Teléfono", "Dirección", "Fecha de Registro"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos (empezar en fila 5)
            int rowNum = 5;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (User client : clients) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, client.getId() != null ? client.getId().toString() : "", dataStyle);
                createCell(row, 1, client.getFullName() != null ? client.getFullName() : "", dataStyle);
                createCell(row, 2, client.getEmail() != null ? client.getEmail() : "", dataStyle);
                createCell(row, 3, client.getPhone() != null ? client.getPhone() : "", dataStyle);
                createCell(row, 4, client.getAddress() != null ? client.getAddress() : "", dataStyle);
                createCell(row, 5, client.getCreatedAt() != null ? client.getCreatedAt().format(formatter) : "", dataStyle);
            }

            // Auto-ajustar columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Exportar productos a Excel
     */
    public byte[] exportProducts() throws IOException {
        List<Product> products = productRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Productos");

            // Agregar logo
            addLogo((XSSFWorkbook) workbook, sheet);

            // Estilo para el header
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Crear header (empezar en fila 4 para dejar espacio al logo)
            Row headerRow = sheet.createRow(4);
            String[] columns = {"ID", "Nombre", "Descripción", "Precio (S/)", "Stock", "Fecha de Creación"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos (empezar en fila 5)
            int rowNum = 5;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, product.getId() != null ? product.getId().toString() : "", dataStyle);
                createCell(row, 1, product.getName() != null ? product.getName() : "", dataStyle);
                createCell(row, 2, product.getDescription() != null ? product.getDescription() : "", dataStyle);
                
                // Precio con formato de moneda
                Cell priceCell = row.createCell(3);
                if (product.getPrice() != null) {
                    priceCell.setCellValue(product.getPrice());
                    priceCell.setCellStyle(currencyStyle);
                } else {
                    priceCell.setCellValue("");
                    priceCell.setCellStyle(dataStyle);
                }
                
                createCell(row, 4, product.getStock() != null ? product.getStock().toString() : "0", dataStyle);
                createCell(row, 5, product.getCreatedAt() != null ? product.getCreatedAt().format(formatter) : "", dataStyle);
            }

            // Auto-ajustar columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Exportar ventas a Excel
     */
    public byte[] exportSales() throws IOException {
        List<Sale> sales = saleRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Ventas");

            // Agregar logo
            addLogo((XSSFWorkbook) workbook, sheet);

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Header
            Row headerRow = sheet.createRow(4);

            String[] columns = {
                    "ID",
                    "Cliente",
                    "Producto",
                    "Cantidad",
                    "Precio Total",
                    "Fecha"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 5;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            if (sales.isEmpty()) {

                Row row = sheet.createRow(rowNum);

                createCell(row, 0, "Sin ventas registradas", dataStyle);

            } else {

                for (Sale sale : sales) {

                    // Si la venta no tiene productos
                    if (sale.getItems() == null || sale.getItems().isEmpty()) {

                        Row row = sheet.createRow(rowNum++);

                        createCell(
                                row,
                                0,
                                sale.getId() != null ? sale.getId().toString() : "",
                                dataStyle);

                        createCell(
                                row,
                                1,
                                sale.getFullName() != null
                                        ? sale.getFullName()
                                        : "",
                                dataStyle);

                        createCell(
                                row,
                                2,
                                "",
                                dataStyle);

                        createCell(
                                row,
                                3,
                                "",
                                dataStyle);

                        Cell totalCell = row.createCell(4);

                        totalCell.setCellValue(
                                sale.getTotal() != null
                                        ? sale.getTotal()
                                        : 0);

                        totalCell.setCellStyle(currencyStyle);

                        createCell(
                                row,
                                5,
                                sale.getCreatedAt() != null
                                        ? sale.getCreatedAt().format(formatter)
                                        : "",
                                dataStyle);

                        continue;
                    }

                    // Una fila por cada producto vendido
                    for (SaleItem item : sale.getItems()) {

                        Row row = sheet.createRow(rowNum++);

                        // ID
                        createCell(
                                row,
                                0,
                                sale.getId() != null
                                        ? sale.getId().toString()
                                        : "",
                                dataStyle);

                        // Cliente
                        createCell(
                                row,
                                1,
                                sale.getFullName() != null
                                        ? sale.getFullName()
                                        : "",
                                dataStyle);

                        // Producto
                        createCell(
                                row,
                                2,
                                item.getProduct() != null
                                        ? item.getProduct().getName()
                                        : "",
                                dataStyle);

                        // Cantidad
                        createCell(
                                row,
                                3,
                                item.getQuantity() != null
                                        ? item.getQuantity().toString()
                                        : "0",
                                dataStyle);

                        // Precio Total
                        Cell totalCell = row.createCell(4);

                        totalCell.setCellValue(
                                sale.getTotal() != null
                                        ? sale.getTotal()
                                        : 0);

                        totalCell.setCellStyle(currencyStyle);

                        // Fecha
                        createCell(
                                row,
                                5,
                                sale.getCreatedAt() != null
                                        ? sale.getCreatedAt().format(formatter)
                                        : "",
                                dataStyle);

                    }

                }

            }

            // Ajustar columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    // Métodos auxiliares para estilos

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("S/ #,##0.00"));
        return style;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Agregar logo de Hurios Rally al Excel
     */
    private void addLogo(XSSFWorkbook workbook, Sheet sheet) {
        try {
            // Intentar cargar el logo desde el proyecto frontend
            File logoFile = new File("../huriosfrontend/public/assets/imgs/logo.webp");
            if (!logoFile.exists()) {
                // Si no existe, intentar desde resources
                logoFile = new File("src/main/resources/static/logo.webp");
            }
            
            if (logoFile.exists()) {
                // Leer la imagen
                InputStream is = new FileInputStream(logoFile);
                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                is.close();
                
                // Crear el dibujo
                XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
                XSSFClientAnchor anchor = new XSSFClientAnchor();
                
                // Posicionar el logo en las primeras filas (A1:B3)
                anchor.setCol1(0);
                anchor.setRow1(0);
                anchor.setCol2(2);
                anchor.setRow2(3);
                
                // Añadir la imagen
                drawing.createPicture(anchor, pictureIdx);
                
                // Ajustar altura de las primeras filas para el logo
                for (int i = 0; i < 4; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        row = sheet.createRow(i);
                    }
                    row.setHeightInPoints(20);
                }
            }
        } catch (Exception e) {
            // Si falla, simplemente no agregar el logo
            System.err.println("No se pudo agregar el logo al Excel: " + e.getMessage());
        }
    }
}
