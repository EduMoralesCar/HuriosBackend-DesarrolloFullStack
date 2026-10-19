# Guía de Pruebas Unitarias - Hurios Rally Project

## 📚 Conceptos Clave

### 1. **JUnit 5**
Framework estándar para pruebas unitarias en Java. Permite:
- Escribir y ejecutar pruebas automatizadas
- Verificar que el código funciona como se espera
- Organizar pruebas con anotaciones como `@Test`, `@BeforeEach`, `@DisplayName`

### 2. **Mockito**
Framework para crear **mocks** (objetos simulados). Útil para:
- Simular dependencias (bases de datos, APIs, servicios externos)
- Aislar la clase que estás probando
- Controlar el comportamiento de las dependencias en tus pruebas

### 3. **TDD (Test-Driven Development)**
Metodología de desarrollo donde:
1. **RED**: Escribes la prueba primero (falla porque no existe el código)
2. **GREEN**: Escribes el código mínimo para que la prueba pase
3. **REFACTOR**: Mejoras el código manteniendo las pruebas pasando

## 🔧 Configuración

Las dependencias ya están en tu `pom.xml`:

```xml
<!-- JUnit 5 y Mockito vienen incluidos en spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito adicional -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

## 🚀 Ejecutar las Pruebas

### Opción 1: Ejecutar todas las pruebas
```bash
mvn test
```

### Opción 2: Ejecutar una clase específica
```bash
mvn test -Dtest=PaymentServiceTest
mvn test -Dtest=ValidationServiceTest
```

### Opción 3: Ejecutar un método específico
```bash
mvn test -Dtest=PaymentServiceTest#testProcessPayment_Success
```

### Opción 4: Con más detalles
```bash
mvn test -X
```

### Opción 5: Usando tu IDE
- **IntelliJ IDEA**: Click derecho en la clase de prueba → "Run Tests"
- **Eclipse**: Click derecho → "Run As" → "JUnit Test"
- **VS Code**: Instala extensión "Java Test Runner"

## 📝 Estructura de las Pruebas Creadas

### 1. `PaymentServiceTest.java`
Pruebas con **Mockito** para servicios con dependencias.

**Ejemplo de mock:**
```java
@Mock
private SaleRepository saleRepository;  // Simulamos el repositorio

@InjectMocks
private PaymentService paymentService;  // Inyectamos los mocks

@Test
void testProcessPayment_Success() {
    // ARRANGE: Configuramos el comportamiento del mock
    when(saleRepository.save(any(Sale.class)))
        .thenReturn(savedSale);
    
    // ACT: Ejecutamos el método
    var response = paymentService.processPayment(request, email);
    
    // ASSERT: Verificamos el resultado
    assertTrue(response.isSuccess());
    verify(saleRepository, times(2)).save(any(Sale.class));
}
```

**Pruebas incluidas:**
- ✅ Procesar pago exitoso
- ❌ Fallar por stock insuficiente
- ❌ Fallar por usuario inexistente
- ❌ Fallar por producto inexistente
- ✅ Obtener ventas de usuario
- ✅ Obtener venta por ID
- ✅ Obtener todas las ventas

### 2. `ValidationServiceTest.java`
Pruebas simples **sin mocks** (no tiene dependencias).

**Ejemplo de prueba parametrizada:**
```java
@ParameterizedTest
@ValueSource(strings = {"invalido", "sin@arroba", "@sindominino.com"})
void testValidateEmail_InvalidFormat(String invalidEmail) {
    // Prueba automáticamente con cada valor del array
    assertThrows(
        IllegalArgumentException.class,
        () -> validationService.validateEmail(invalidEmail)
    );
}
```

**Pruebas incluidas:**
- ✅ Validar emails, contraseñas, precios
- ✅ Validar nombres de productos y stock
- ✅ Validar teléfonos y rangos numéricos
- ✅ Normalizar y sanitizar strings

## 🎯 Anotaciones Principales

### Anotaciones de JUnit
| Anotación | Descripción |
|-----------|-------------|
| `@Test` | Marca un método como prueba |
| `@BeforeEach` | Se ejecuta antes de cada prueba |
| `@AfterEach` | Se ejecuta después de cada prueba |
| `@DisplayName` | Nombre legible para la prueba |
| `@ParameterizedTest` | Prueba con múltiples valores |

### Anotaciones de Mockito
| Anotación | Descripción |
|-----------|-------------|
| `@Mock` | Crea un objeto simulado |
| `@InjectMocks` | Inyecta los mocks en la clase a probar |
| `@ExtendWith(MockitoExtension.class)` | Habilita Mockito en JUnit 5 |

## 🧪 Métodos de Aserciones (Assertions)

```java
// Verificar valores
assertEquals(expected, actual);
assertNotEquals(expected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(object);
assertNotNull(object);

// Verificar excepciones
assertThrows(Exception.class, () -> {
    // código que debe lanzar excepción
});

// Verificar que NO lanza excepción
assertDoesNotThrow(() -> {
    // código que no debe fallar
});
```

## 🎭 Métodos de Mockito

```java
// Configurar comportamiento del mock
when(mock.metodo()).thenReturn(valor);
when(mock.metodo()).thenThrow(new Exception());

// Verificar llamadas
verify(mock).metodo();                    // Se llamó una vez
verify(mock, times(2)).metodo();         // Se llamó 2 veces
verify(mock, never()).metodo();          // Nunca se llamó
verify(mock, atLeastOnce()).metodo();    // Al menos una vez

// Matchers (comodines)
any()           // Cualquier objeto
anyString()     // Cualquier String
anyInt()        // Cualquier Integer
eq(valor)       // Valor específico
```

## 📊 Interpretar Resultados

### Salida exitosa:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Salida con errores:
```
[ERROR] Tests run: 8, Failures: 1, Errors: 0, Skipped: 0
[ERROR] testProcessPayment_Success  Time elapsed: 0.123 s  <<< FAILURE!
Expected: true
Actual: false
```

## 🔄 Ciclo TDD en Práctica

### Ejemplo: Agregar una nueva validación

1. **Escribir la prueba primero:**
```java
@Test
void testValidateDNI_Valid() {
    assertDoesNotThrow(() -> 
        validationService.validateDNI("12345678")
    );
}
```

2. **Ejecutar (debe fallar):**
```bash
mvn test -Dtest=ValidationServiceTest#testValidateDNI_Valid
```

3. **Escribir el código:**
```java
public void validateDNI(String dni) {
    Preconditions.checkArgument(
        dni != null && dni.matches("\\d{8}"),
        "DNI debe tener 8 dígitos"
    );
}
```

4. **Ejecutar nuevamente (debe pasar):**
```bash
mvn test -Dtest=ValidationServiceTest#testValidateDNI_Valid
```

5. **Refactorizar si es necesario**

## 💡 Consejos

1. **Nombres descriptivos**: Usa `@DisplayName` para describir claramente qué prueba cada test
2. **Arrange-Act-Assert**: Organiza tus pruebas en 3 secciones claras
3. **Una cosa a la vez**: Cada test debe probar UN comportamiento específico
4. **Independencia**: Los tests no deben depender entre sí
5. **Prueba casos límite**: Null, vacío, valores negativos, etc.
6. **Cobertura**: Intenta cubrir casos exitosos Y casos de error

## 📚 Recursos Adicionales

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TDD by Example - Kent Beck](https://www.amazon.com/Test-Driven-Development-Kent-Beck/dp/0321146530)

## 🏃 Próximos Pasos

Para crear más pruebas, sigue el patrón de las pruebas existentes:

1. **Para servicios con dependencias** → Usa el patrón de `PaymentServiceTest`
2. **Para utilidades sin dependencias** → Usa el patrón de `ValidationServiceTest`
3. **Para probar múltiples valores** → Usa `@ParameterizedTest`

¡Buena suerte con tus pruebas! 🎉
