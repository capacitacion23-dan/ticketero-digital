# 05 - LOMBOK BEST PRACTICES


## OBJETIVO: REDUCIR BOILERPLATE CORRECTAMENTE


Lombok genera código automáticamente, pero debe usarse con cuidado para evitar problemas.


---


## ✅ ANOTACIONES RECOMENDADAS


### @RequiredArgsConstructor (Dependency Injection)


```java
// ✅ CORRECTO: Constructor injection con Lombok
@Service
@RequiredArgsConstructor
public class UserService {
   private final UserRepository userRepository;
   private final EmailService emailService;
  
   // Lombok genera automáticamente:
   // public UserService(UserRepository userRepository, EmailService emailService) {
   //     this.userRepository = userRepository;
   //     this.emailService = emailService;
   // }
}
```


**Por qué:**
- ✅ Inmutabilidad (final fields)
- ✅ Thread-safe
- ✅ Spring inyecta automáticamente
- ✅ Menos líneas de código


---


### @Slf4j (Logging)


```java
// ✅ CORRECTO: Logger con Lombok
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
  
   public OrderResponse create(OrderRequest request) {
       log.info("Creating order for customer: {}", request.customerId());
       // ...
       log.debug("Order created with ID: {}", savedOrder.getId());
       return response;
   }
}


// ❌ INCORRECTO: Logger manual
@Service
public class OrderService {
   private static final Logger log = LoggerFactory.getLogger(OrderService.class);
}
```


**Niveles de logging:**
- `log.error()` - Errores críticos
- `log.warn()` - Advertencias
- `log.info()` - Información importante
- `log.debug()` - Debugging (desarrollo)
- `log.trace()` - Tracing detallado


---


### @Builder (Construcción de Objetos)


```java
// ✅ CORRECTO: Builder pattern
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   private String name;
   private BigDecimal price;
   private Integer stock;
}


// Uso
Product product = Product.builder()
   .name("Laptop")
   .price(BigDecimal.valueOf(999.99))
   .stock(50)
   .build();
```


**Cuándo usar @Builder:**
- ✅ Entities con muchos campos
- ✅ Objetos inmutables
- ✅ Testing (crear objetos de prueba)
- ❌ NO en DTOs (usar Records)


---


### @Data (Con Precaución)


```java
// ✅ CORRECTO: Entity simple sin relaciones
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   private String name;
   private String description;
}
```


**@Data genera:**
- Getters para todos los campos
- Setters para campos no-final
- `toString()`
- `equals()` y `hashCode()`
- Constructor con campos requeridos


**Precaución:**
- ⚠️ NO usar en entities con relaciones bidireccionales
- ⚠️ `toString()` puede causar lazy loading
- ⚠️ `equals()/hashCode()` con relaciones causa problemas


---


### @ToString.Exclude (Relaciones JPA)


```java
// ✅ CORRECTO: Excluir relaciones del toString
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   private Long customerId;
   private BigDecimal total;
  
   @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
   @ToString.Exclude  // ← CRÍTICO
   private List<OrderItem> items = new ArrayList<>();
}
```


**Por qué @ToString.Exclude:**
- ✅ Evita lazy loading exceptions
- ✅ Evita recursión infinita
- ✅ Evita N+1 queries accidentales


---


### @EqualsAndHashCode.Exclude


```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @EqualsAndHashCode.Exclude  // No incluir en equals/hashCode
   private Long id;
  
   @Column(unique = true)
   private String email;  // ← Business key para equals
  
   @OneToMany(mappedBy = "user")
   @ToString.Exclude
   @EqualsAndHashCode.Exclude  // ← CRÍTICO
   private List<Order> orders = new ArrayList<>();
}
```


**Reglas equals/hashCode en JPA:**
- ✅ Usar business key (email, código único)
- ✅ Excluir ID generado
- ✅ Excluir relaciones
- ❌ NO incluir campos mutables en equals


---


## 🚫 ANOTACIONES A EVITAR


### ❌ @Data en Entities con Relaciones


```java
// ❌ INCORRECTO
@Entity
@Data  // toString() causará lazy loading
public class Order {
   @OneToMany(mappedBy = "order")
   private List<OrderItem> items;  // Problema!
}


// ✅ CORRECTO
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
   @OneToMany(mappedBy = "order")
   @ToString.Exclude
   @EqualsAndHashCode.Exclude
   private List<OrderItem> items;
}
```


### ❌ @AllArgsConstructor sin @NoArgsConstructor en JPA


```java
// ❌ INCORRECTO: JPA requiere constructor sin argumentos
@Entity
@AllArgsConstructor  // Solo este
public class User {
   // JPA no puede instanciar
}


// ✅ CORRECTO
@Entity
@NoArgsConstructor  // Requerido por JPA
@AllArgsConstructor  // Útil para builder
@Builder
public class User {
   // ...
}
```


### ❌ @Value (Inmutabilidad Total)


```java
// ❌ NO usar @Value en entities
@Entity
@Value  // Hace todo final
public class Product {
   Long id;
   String name;
   // JPA no puede setear valores!
}


// ✅ Usar @Value solo para Value Objects
@Value
public class Money {
   BigDecimal amount;
   String currency;
}
```


---


## ✅ COMBINACIONES RECOMENDADAS


### Para Entities JPA
```java
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   private String name;
   private BigDecimal price;
}
```


### Para Services
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
   private final ProductRepository productRepository;
  
   public ProductResponse create(ProductRequest request) {
       log.info("Creating product: {}", request.name());
       // ...
   }
}
```


### Para Controllers
```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
   private final ProductService productService;
  
   @PostMapping
   public ResponseEntity<ProductResponse> create(
       @Valid @RequestBody ProductRequest request
   ) {
       log.info("POST /api/products - request: {}", request);
       return ResponseEntity.status(201)
           .body(productService.create(request));
   }
}
```


### Para Value Objects
```java
@Value
@Builder
public class Address {
   String street;
   String city;
   String postalCode;
   String country;
}
```


---


## 🎯 LOMBOK EN TESTING


### Test Data Builders
```java
@Builder
public class UserTestData {
   @Builder.Default
   private String email = "test@example.com";
  
   @Builder.Default
   private String firstName = "John";
  
   @Builder.Default
   private String lastName = "Doe";
  
   public User build() {
       return User.builder()
           .email(email)
           .firstName(firstName)
           .lastName(lastName)
           .build();
   }
}


// Uso en tests
@Test
void testCreateUser() {
   User user = UserTestData.builder()
       .email("custom@example.com")
       .build()
       .build();
  
   // ...
}
```


---


## 📋 CONFIGURACIÓN LOMBOK


### lombok.config (Raíz del proyecto)
```properties
# Configuración global de Lombok
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true


# Logging
lombok.log.fieldName = log
lombok.log.fieldIsStatic = true


# Builder
lombok.builder.className = Builder


# ToString
lombok.toString.doNotUseGetters = true
lombok.toString.includeFieldNames = true
```


### Maven Dependency
```xml
<dependency>
   <groupId>org.projectlombok</groupId>
   <artifactId>lombok</artifactId>
   <version>1.18.30</version>
   <scope>provided</scope>
</dependency>
```


### Gradle Dependency
```gradle
compileOnly 'org.projectlombok:lombok:1.18.30'
annotationProcessor 'org.projectlombok:lombok:1.18.30'
```


---


## 🚫 ANTI-PATTERNS COMUNES


### ❌ Lombok + Relaciones Bidireccionales
```java
// ❌ INCORRECTO
@Entity
@Data  // toString() causa recursión infinita
public class Parent {
   @OneToMany(mappedBy = "parent")
   private List<Child> children;
}


@Entity
@Data
public class Child {
   @ManyToOne
   private Parent parent;  // Recursión!
}


// ✅ CORRECTO
@Entity
@Getter
@Setter
public class Parent {
   @OneToMany(mappedBy = "parent")
   @ToString.Exclude
   private List<Child> children;
}
```


### ❌ @Data en DTOs (Usar Records)
```java
// ❌ INCORRECTO (Java 17+)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
   private Long id;
   private String email;
}


// ✅ CORRECTO
public record UserResponse(Long id, String email) {}
```


---


## 🎯 CHECKLIST LOMBOK


Antes de usar Lombok:


- [ ] ¿Es Entity con relaciones? → NO @Data
- [ ] ¿Necesitas logging? → @Slf4j
- [ ] ¿Dependency injection? → @RequiredArgsConstructor
- [ ] ¿Constructor builder? → @Builder
- [ ] ¿Relaciones JPA? → @ToString.Exclude + @EqualsAndHashCode.Exclude
- [ ] ¿Es DTO? → Usar Record, NO Lombok
- [ ] ¿JPA entity? → @NoArgsConstructor requerido


---


## 💡 REGLAS FINALES


1. **@RequiredArgsConstructor** para todos los services/controllers
2. **@Slf4j** cuando necesites logging
3. **@Builder** para entities y test data
4. **NO @Data** en entities con relaciones
5. **@ToString.Exclude** en TODAS las relaciones JPA
6. **@EqualsAndHashCode.Exclude** en ID y relaciones
7. **Records** en lugar de Lombok para DTOs
8. **@NoArgsConstructor** siempre en entities JPA


---


**Versión:** 1.0 
**Lombok:** 1.18.30+ 
**Enfoque:** Uso seguro y productivo