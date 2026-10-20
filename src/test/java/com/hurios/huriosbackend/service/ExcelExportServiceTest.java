package com.hurios.huriosbackend.service;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.entity.User;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para ExcelExportService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelExportService - Pruebas Unitarias")
class ExcelExportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ExcelExportService excelExportService;

    @Test
    @DisplayName("exportClients debe generar un Excel con cabeceras y datos de clientes")
    void testExportClients_GeneratesExcel() throws IOException {
        // ARRANGE
        User client = new User();
        client.setId(1L);
        client.setFullName("Cliente Uno");
        client.setEmail("cliente@example.com");
        client.setPhone("987654321");
        client.setAddress("Calle Falsa 123");
        client.setRole("CLIENTE");
        client.setCreatedAt(LocalDateTime.now());

        when(userRepository.findAll()).thenReturn(List.of(client));

        // ACT
        byte[] excelBytes = excelExportService.exportClients();

        // ASSERT
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "El archivo Excel no debe estar vacío");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Clientes");
            assertNotNull(sheet, "La hoja 'Clientes' debe existir");

            // Header en fila 4 (índice 4, empezando en 0)
            Row headerRow = sheet.getRow(4);
            assertNotNull(headerRow, "La fila de cabecera debe existir");
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Nombre Completo", headerRow.getCell(1).getStringCellValue());

            // Primera fila de datos en fila 5
            Row dataRow = sheet.getRow(5);
            assertNotNull(dataRow, "La fila de datos debe existir");
            assertEquals("Cliente Uno", dataRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("exportProducts debe generar un Excel con cabeceras y datos de productos")
    void testExportProducts_GeneratesExcel() throws IOException {
        // ARRANGE
        Product product = new Product();
        product.setId(10L);
        product.setName("Producto Test");
        product.setDescription("Descripción del producto");
        product.setPrice(50.0);
        product.setStock(5);
        product.setCreatedAt(LocalDateTime.now());

        when(productRepository.findAll()).thenReturn(List.of(product));

        // ACT
        byte[] excelBytes = excelExportService.exportProducts();

        // ASSERT
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "El archivo Excel no debe estar vacío");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Productos");
            assertNotNull(sheet, "La hoja 'Productos' debe existir");

            Row headerRow = sheet.getRow(4);
            assertNotNull(headerRow, "La fila de cabecera debe existir");
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Nombre", headerRow.getCell(1).getStringCellValue());
            assertEquals("Precio (S/)", headerRow.getCell(3).getStringCellValue());

            Row dataRow = sheet.getRow(5);
            assertNotNull(dataRow, "La fila de datos debe existir");
            assertEquals("Producto Test", dataRow.getCell(1).getStringCellValue());
        }
    }
}
