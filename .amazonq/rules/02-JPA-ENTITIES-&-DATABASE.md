# 02 - JPA ENTITIES & DATABASE


## BUENAS PRÁCTICAS JPA/HIBERNATE


Guía genérica para crear entities correctas en cualquier proyecto Spring Boot.


---


## ✅ ENTITY PATTERN BÁSICO


```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   @Column(unique = true, nullable = false, length = 100)
   private String email;
  
   @Column(nullable = false, length = 50)
   private String firstName;
  
   @Column(nullable = false, length = 50)
   private String lastName;
  
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private UserStatus status;
  
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;
  
   @Column(name = "updated_at")
   private LocalDateTime updatedAt;
  
   @PrePersist
   protected void onCreate() {
       this.createdAt = LocalDateTime.now();
       this.status = UserStatus.ACTIVE;
   }
  
   @PreUpdate
   protected void onUpdate() {
       this.updatedAt = LocalDateTime.now();
   }
}
```


**Reglas Entity Básicas:**
- ✅ `@Table(name = "snake_case")` explícito
- ✅ `@Column` con constraints (nullable, length, unique)
- ✅ `@PrePersist` y `@PreUpdate` para timestamps
- ✅ Enums con `EnumType.STRING` (NO ORDINAL)
- ✅ Names descriptivos en snake_case para columnas
- ✅ Lombok para reducir boilerplate


---


## 🔗 RELACIONES JPA


### OneToMany / ManyToOne (Bidireccional)


```java
// Lado "One" (Parent)
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
  
   @Column(nullable = false)
   private Long customerId;
  
   // Relación 1:N
   @OneToMany(
       mappedBy = "order",           // Campo en OrderItem
       cascade = CascadeType.ALL,    // Propagar operaciones
       orphanRemoval = true          // Eliminar huérfanos
   )
   @ToString.Exclude                 // Evitar lazy loading en toString
   private List<OrderItem> items = new ArrayList<>();
}


// Lado "Many" (Child)
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   @Column(nullable = false)
   private Long productId;
  
   @Column(nullable = false)
   private Integer quantity;
  
   // Relación N:1
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "order_id", nullable = false)
   @ToString.Exclude
   private Order order;
}
```


**Reglas Relaciones:**
- ✅ `mappedBy` en lado @OneToMany (owner de relación)
- ✅ `@JoinColumn` en lado @ManyToOne
- ✅ `FetchType.LAZY` por defecto (performance)
- ✅ `@ToString.Exclude` en AMBOS lados (evita ciclos)
- ✅ `cascade = CascadeType.ALL` con cuidado
- ✅ Inicializar listas: `= new ArrayList<>()`
- ❌ NO `FetchType.EAGER` sin justificación


### ManyToMany


```java
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   @Column(nullable = false)
   private String name;
  
   @ManyToMany
   @JoinTable(
       name = "student_courses",
       joinColumns = @JoinColumn(name = "student_id"),
       inverseJoinColumns = @JoinColumn(name = "course_id")
   )
   @ToString.Exclude
   private Set<Course> courses = new HashSet<>();
}


@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
  
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;
  
   @Column(nullable = false)
   private String title;
  
   @ManyToMany(mappedBy = "courses")
   @ToString.Exclude
   private Set<Student> students = new HashSet<>();
}
```


**Reglas ManyToMany:**
- ✅ Usar `Set<>` en lugar de `List<>` (evita duplicados)
- ✅ `@JoinTable` en un lado solamente
- ✅ `mappedBy` en el otro lado
- ✅ Tabla intermedia con naming claro


---


## 📋 ENUMS


```java
public enum UserStatus {
   ACTIVE,
   INACTIVE,
   SUSPENDED,
   DELETED
}


public enum OrderStatus {
   PENDING,
   PROCESSING,
   SHIPPED,
   DELIVERED,
   CANCELLED
}
```


**Uso en Entity:**
```java
@Enumerated(EnumType.STRING)  // ✅ STRING, NO ORDINAL
@Column(nullable = false, length = 20)
private UserStatus status;
```


**Por qué STRING:**
- ✅ Legible en base de datos
- ✅ Refactoring-safe (agregar/reordenar enums)
- ❌ ORDINAL se rompe si reordenas enums


---


## 🗄️ FLYWAY MIGRATIONS


### Nomenclatura
```
V1__create_users_table.sql
V2__create_orders_table.sql
V3__add_email_index_to_users.sql
V4__alter_orders_add_tracking_number.sql
```


### Ejemplo Migration Completa
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,
   email VARCHAR(100) UNIQUE NOT NULL,
   first_name VARCHAR(50) NOT NULL,
   last_name VARCHAR(50) NOT NULL,
   status VARCHAR(20) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP
);


-- Índices para performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);


-- V2__create_orders_table.sql
CREATE TABLE orders (
   id BIGSERIAL PRIMARY KEY,
   customer_id BIGINT NOT NULL,
   total DECIMAL(10,2) NOT NULL,
   status VARCHAR(20) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   CONSTRAINT fk_customer FOREIGN KEY (customer_id)
       REFERENCES users(id) ON DELETE CASCADE
);


CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
```


**Reglas Flyway:**
- ✅ `V{VERSION}__{description}.sql`
- ✅ Versión incremental (V1, V2, V3...)
- ✅ Nombres descriptivos
- ✅ Un objetivo por migration
- ✅ Índices en columnas de búsqueda
- ✅ Foreign keys explícitas
- ❌ NO modificar migrations ya aplicadas


---


## 🔍 REPOSITORY QUERIES


### Queries Derivadas (Preferir siempre)
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  
   // Spring genera automáticamente el SQL
   Optional<User> findByEmail(String email);
  
   List<User> findByStatus(UserStatus status);
  
   List<User> findByLastNameContainingIgnoreCase(String lastName);
  
   List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
  
   boolean existsByEmail(String email);
  
   long countByStatus(UserStatus status);
  
   void deleteByStatus(UserStatus status);
}
```


### @Query para Casos Complejos
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
  
   // JPQL con joins
   @Query("""
       SELECT o FROM Order o
       JOIN FETCH o.items
       WHERE o.customerId = :customerId
       AND o.status = :status
       """)
   List<Order> findByCustomerAndStatus(
       @Param("customerId") Long customerId,
       @Param("status") OrderStatus status
   );
  
   // Query con paginación
   @Query("""
       SELECT o FROM Order o
       WHERE o.createdAt > :date
       ORDER BY o.createdAt DESC
       """)
   Page<Order> findRecentOrders(
       @Param("date") LocalDateTime date,
       Pageable pageable
   );
  
   // Query nativa (último recurso)
   @Query(value = """
       SELECT * FROM orders
       WHERE status = ?1
       AND total > ?2
       ORDER BY created_at DESC
       LIMIT ?3
       """, nativeQuery = true)
   List<Order> findTopOrdersByStatusAndMinTotal(
       String status,
       BigDecimal minTotal,
       int limit
   );
}
```


**Reglas Queries:**
- ✅ Preferir query derivadas (Spring las genera)
- ✅ `@Query` solo para queries complejas
- ✅ JPQL sobre queries nativas
- ✅ `JOIN FETCH` para evitar N+1
- ✅ `@Param` para parámetros nombrados
- ✅ Text blocks `"""` para queries largas
- ❌ NO queries nativas sin razón fuerte


---


## 📊 ÍNDICES Y PERFORMANCE


### Cuándo Crear Índices
```sql
-- ✅ Columnas en WHERE frecuentemente
CREATE INDEX idx_users_email ON users(email);


-- ✅ Columnas en ORDER BY
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);


-- ✅ Foreign Keys
CREATE INDEX idx_orders_customer_id ON orders(customer_id);


-- ✅ Índices compuestos para búsquedas combinadas
CREATE INDEX idx_orders_status_date ON orders(status, created_at);


-- ❌ NO indexar columnas con pocos valores únicos (ej: boolean)
-- ❌ NO sobre-indexar (afecta INSERT/UPDATE)
```


### Evitar N+1 Problem
```java
// ❌ MAL: N+1 queries
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> {
   // Lazy load dispara 1 query por orden
   order.getItems().size();
});


// ✅ BIEN: JOIN FETCH (1 query)
@Query("SELECT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();
```


---


## 🎯 VALIDACIONES JPA


```java
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
  
   @NotBlank(message = "Name is required")
   @Size(min = 3, max = 100, message = "Name must be between 3-100 characters")
   @Column(nullable = false, length = 100)
   private String name;
  
   @NotNull(message = "Price is required")
   @DecimalMin(value = "0.01", message = "Price must be greater than 0")
   @Column(nullable = false, precision = 10, scale = 2)
   private BigDecimal price;
  
   @Min(value = 0, message = "Stock cannot be negative")
   @Column(nullable = false)
   private Integer stock;
}
```


**Nota:** Validaciones Jakarta son mejores en DTOs con `@Valid` en controller.


---


## 🎯 CHECKLIST ENTITIES


Antes de crear entity:


- [ ] `@Entity` + `@Table(name = "snake_case")`
- [ ] `@Id` + `@GeneratedValue(strategy = IDENTITY)`
- [ ] `@Column` con constraints (nullable, length, unique)
- [ ] `@PrePersist` / `@PreUpdate` para timestamps
- [ ] Enums con `EnumType.STRING`
- [ ] Relaciones con `@ToString.Exclude`
- [ ] `FetchType.LAZY` en relaciones
- [ ] Inicializar colecciones (`= new ArrayList<>()`)
- [ ] Migration Flyway creada
- [ ] Índices en columnas de búsqueda


---


**Versión:** 2.0 
**Database:** PostgreSQL (patterns aplicables a cualquier RDBMS) 
**Enfoque:** Buenas prácticas genéricas JPA/Hibernate