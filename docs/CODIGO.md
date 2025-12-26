# Documentación de Código - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Stack:** Java 21 + Spring Boot 3.2.11 + PostgreSQL 16

---

## Stack Tecnológico

### Backend
- **Java:** 21 LTS (OpenJDK)
- **Framework:** Spring Boot 3.2.11
- **Build Tool:** Maven 3.9+
- **Base de Datos:** PostgreSQL 16
- **Migraciones:** Flyway
- **Containerización:** Docker + Docker Compose

### Dependencias Principales
```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web           # REST API
spring-boot-starter-data-jpa      # JPA/Hibernate
spring-boot-starter-validation    # Bean Validation
spring-boot-starter-actuator      # Health checks

<!-- Database -->
postgresql                        # PostgreSQL driver
flyway-core                       # Database migrations

<!-- Utilities -->
lombok                           # Reduce boilerplate
```

### Dependencias de Testing
```xml
spring-boot-starter-test         # Testing framework
testcontainers-junit-jupiter     # Integration testing
testcontainers-postgresql        # Database testing
rest-assured                     # API testing
wiremock-standalone             # API mocking
awaitility                      # Async testing
```

---

## Estructura del Proyecto

### Organización de Packages
```
com.example.ticketero/
├── TicketeroApplication.java           # @SpringBootApplication + @EnableScheduling
│
├── controller/                         # @RestController - Capa de presentación
│   ├── TicketController.java          # Endpoints públicos de tickets
│   ├── AdminController.java           # Endpoints administrativos
│   └── GlobalExceptionHandler.java    # @ControllerAdvice - Manejo global de errores
│
├── service/                           # @Service - Lógica de negocio
│   ├── TicketService.java            # Gestión de tickets
│   ├── AdvisorService.java           # Gestión de asesores
│   ├── MessageService.java           # Gestión de mensajes
│   ├── QueueProcessorService.java    # Procesamiento de colas
│   └── DashboardService.java         # Datos del dashboard
│
├── repository/                        # @Repository - Acceso a datos
│   ├── TicketRepository.java         # JpaRepository<Ticket, Long>
│   ├── AdvisorRepository.java        # JpaRepository<Advisor, Long>
│   └── MensajeRepository.java        # JpaRepository<Mensaje, Long>
│
├── model/
│   ├── entity/                       # @Entity - Entidades JPA
│   │   ├── Ticket.java              # Entidad principal
│   │   ├── Advisor.java             # Asesores/ejecutivos
│   │   └── Mensaje.java             # Mensajes programados
│   │
│   ├── dto/
│   │   ├── request/                  # DTOs de entrada
│   │   │   ├── CreateTicketRequest.java
│   │   │   └── UpdateTicketStatusRequest.java
│   │   └── response/                 # DTOs de salida
│   │       ├── TicketResponse.java
│   │       ├── AdvisorResponse.java
│   │       ├── DashboardResponse.java
│   │       └── ErrorResponse.java
│   │
│   └── enums/                        # Enumeraciones
│       ├── QueueType.java           # CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA
│       ├── TicketStatus.java        # EN_ESPERA, PROXIMO, ATENDIENDO, etc.
│       ├── AdvisorStatus.java       # AVAILABLE, BUSY, OFFLINE
│       └── MessageTemplate.java     # Plantillas de mensajes
│
└── scheduler/                        # @Component + @Scheduled
    ├── MessageScheduler.java        # Procesamiento de mensajes (60s)
    └── QueueProcessorScheduler.java # Asignación automática (5s)
```

### Archivos de Configuración
```
src/main/resources/
├── application.yml                  # Configuración principal
└── db/migration/                   # Migraciones Flyway
    ├── V1__create_ticket_table.sql
    ├── V2__create_mensaje_table.sql
    └── V3__create_advisor_table.sql
```

---

## Capas del Sistema

### 1. Controller Layer (Presentación)

**Responsabilidades:**
- Recibir requests HTTP
- Validar entrada con `@Valid`
- Delegar a services
- Retornar responses HTTP

**Patrones Aplicados:**
```java
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor  // Constructor injection
@Slf4j                   // Logging
public class TicketController {
    
    private final TicketService ticketService;
    
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
        @Valid @RequestBody CreateTicketRequest request
    ) {
        log.info("POST /api/tickets - Creating ticket for {}", request.nationalId());
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(201).body(response);
    }
}
```

**Endpoints Implementados:**
- `POST /api/tickets` - Crear ticket
- `GET /api/tickets/{id}` - Obtener ticket por ID
- `GET /api/tickets/codigo/{uuid}` - Obtener por código referencia
- `GET /api/tickets/numero/{numero}` - Obtener por número
- `GET /api/tickets` - Listar con filtros
- `PUT /api/tickets/{id}/status` - Actualizar estado
- `GET /api/admin/dashboard` - Dashboard administrativo
- `GET /api/admin/advisors` - Lista de asesores

### 2. Service Layer (Lógica de Negocio)

**Responsabilidades:**
- Implementar reglas de negocio
- Coordinar operaciones
- Gestionar transacciones
- Transformar entities ↔ DTOs

**Patrones Aplicados:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // Por defecto lectura
public class TicketService {
    
    private final TicketRepository ticketRepository;
    
    @Transactional  // Escritura requiere anotación explícita
    public TicketResponse createTicket(CreateTicketRequest request) {
        // Lógica de negocio
        // Transformación DTO → Entity
        // Persistencia
        // Transformación Entity → DTO
    }
    
    private TicketResponse toResponse(Ticket ticket) {
        // Mapper manual Entity → DTO
    }
}
```

**Services Implementados:**
- `TicketService` - Gestión completa de tickets
- `AdvisorService` - Gestión de asesores
- `MessageService` - Gestión de mensajes
- `QueueProcessorService` - Procesamiento de colas
- `DashboardService` - Agregación de datos

### 3. Repository Layer (Acceso a Datos)

**Responsabilidades:**
- Acceso a base de datos
- Queries personalizadas
- Solo operaciones de datos

**Patrones Aplicados:**
```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // Query derivada (Spring genera SQL automáticamente)
    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);
    
    // Query custom con @Query
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.queueType = :queueType 
        AND t.status IN :statuses
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findActiveTicketsByQueueType(
        @Param("queueType") QueueType queueType,
        @Param("statuses") List<TicketStatus> statuses
    );
}
```

### 4. Entity Layer (Persistencia)

**Responsabilidades:**
- Mapeo objeto-relacional
- Relaciones entre entidades
- Validaciones de integridad

**Patrones Aplicados:**
```java
@Entity
@Table(name = "ticket")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)  // STRING, no ORDINAL
    @Column(name = "queue_type", nullable = false)
    private QueueType queueType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    @ToString.Exclude  // Evitar lazy loading en toString
    private Advisor assignedAdvisor;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.codigoReferencia == null) {
            this.codigoReferencia = UUID.randomUUID();
        }
    }
}
```

---

## Patrones Aplicados

### Spring Boot Patterns

#### 1. Dependency Injection
```java
// ✅ Constructor Injection (Recomendado)
@Service
@RequiredArgsConstructor  // Lombok genera constructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final MessageService messageService;
}

// ❌ Field Injection (No usar)
@Autowired
private TicketRepository ticketRepository;
```

#### 2. Configuration Management
```yaml
# application.yml - Configuración externalizada
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ticketero}
    username: ${DATABASE_USERNAME:dev}
    password: ${DATABASE_PASSWORD:dev123}

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN:}
  api-url: https://api.telegram.org/bot
```

#### 3. Exception Handling
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        // Manejo centralizado de errores de validación
    }
}
```

### JPA/Hibernate Patterns

#### 1. Entity Relationships
```java
// OneToMany - Lado "One"
@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
@ToString.Exclude
@Builder.Default
private List<Mensaje> mensajes = new ArrayList<>();

// ManyToOne - Lado "Many"
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ticket_id", nullable = false)
@ToString.Exclude
private Ticket ticket;
```

#### 2. Enum Mapping
```java
@Enumerated(EnumType.STRING)  // ✅ STRING para legibilidad
@Column(name = "queue_type", nullable = false)
private QueueType queueType;
```

#### 3. Lifecycle Callbacks
```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.codigoReferencia = UUID.randomUUID();
}

@PreUpdate
protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

### DTO Patterns

#### 1. Request DTOs con Validación
```java
public record CreateTicketRequest(
    @NotBlank(message = "National ID is required")
    @Size(min = 7, max = 20)
    String nationalId,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    String telefono,
    
    @NotNull(message = "Queue type is required")
    QueueType queueType
) {}
```

#### 2. Response DTOs Inmutables
```java
public record TicketResponse(
    Long id,
    UUID codigoReferencia,
    String numero,
    QueueType queueType,
    TicketStatus status,
    Integer positionInQueue,
    LocalDateTime createdAt
) {}
```

### Java 21 Features

#### 1. Records para DTOs
```java
// ✅ Record (Java 17+)
public record TicketResponse(Long id, String numero) {}

// ❌ Clase tradicional con boilerplate
public class TicketResponse {
    private Long id;
    private String numero;
    // getters, setters, equals, hashCode...
}
```

#### 2. Text Blocks para Queries
```java
@Query("""
    SELECT t FROM Ticket t
    LEFT JOIN FETCH t.assignedAdvisor
    WHERE t.status = :status
    AND t.createdAt > :date
    ORDER BY t.createdAt DESC
    """)
List<Ticket> findActiveTickets(@Param("status") TicketStatus status);
```

#### 3. Switch Expressions
```java
public char getPrefix() {
    return switch (this) {
        case CAJA -> 'C';
        case PERSONAL_BANKER -> 'P';
        case EMPRESAS -> 'E';
        case GERENCIA -> 'G';
    };
}
```

### Lombok Patterns

#### 1. Entity Annotations
```java
@Entity
@Getter @Setter           // Getters/setters
@NoArgsConstructor        // Constructor sin argumentos (JPA)
@AllArgsConstructor       // Constructor con todos los argumentos
@Builder                  // Builder pattern
public class Ticket {
    // campos
}
```

#### 2. Service Annotations
```java
@Service
@RequiredArgsConstructor  // Constructor con final fields
@Slf4j                   // Logger automático
public class TicketService {
    private final TicketRepository repository;
    
    public void method() {
        log.info("Logging with Lombok");
    }
}
```

#### 3. Exclusiones para JPA
```java
@OneToMany(mappedBy = "ticket")
@ToString.Exclude         // Evitar lazy loading
@EqualsAndHashCode.Exclude // Evitar recursión
private List<Mensaje> mensajes;
```

---

## Convenciones de Código

### Naming Conventions

#### 1. Packages
```java
com.example.ticketero.controller    // Singular, no controllers
com.example.ticketero.service       // Singular, no services
com.example.ticketero.repository    // Singular, no repositories
```

#### 2. Classes
```java
// Controllers
TicketController        // Entidad + Controller
AdminController         // Funcionalidad + Controller

// Services
TicketService          // Entidad + Service
QueueProcessorService  // Funcionalidad + Service

// Repositories
TicketRepository       // Entidad + Repository

// DTOs
CreateTicketRequest    // Acción + Entidad + Request
TicketResponse         // Entidad + Response
```

#### 3. Methods
```java
// Repository queries
findByCodigoReferencia()     // findBy + Campo
findActiveTicketsByQueueType() // findBy + Descripción

// Service methods
createTicket()               // Acción + Entidad
calculateQueuePosition()     // Acción + Descripción
toResponse()                 // Mapper methods
```

#### 4. Database
```sql
-- Tablas en singular
CREATE TABLE ticket (...);
CREATE TABLE advisor (...);

-- Columnas en snake_case
codigo_referencia UUID
national_id VARCHAR(20)
created_at TIMESTAMP

-- Índices descriptivos
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
```

### Code Style

#### 1. Imports Organization
```java
// Java standard library
import java.time.LocalDateTime;
import java.util.List;

// Third-party libraries
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Project imports
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.repository.TicketRepository;
```

#### 2. Method Organization
```java
@Service
public class TicketService {
    
    // Public methods first
    public TicketResponse createTicket() { }
    public Optional<TicketResponse> findById() { }
    
    // Private methods last
    private String generateTicketNumber() { }
    private TicketResponse toResponse() { }
}
```

#### 3. Logging Conventions
```java
// Info para operaciones importantes
log.info("Creating ticket for nationalId: {}", request.nationalId());

// Debug para detalles técnicos
log.debug("Generated ticket number: {}", numero);

// Error para excepciones
log.error("Failed to create ticket: {}", e.getMessage(), e);
```

### Validation Patterns

#### 1. Bean Validation
```java
public record CreateTicketRequest(
    @NotBlank(message = "National ID is required")
    @Size(min = 7, max = 20, message = "National ID must be 7-20 characters")
    String nationalId,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    String telefono
) {}
```

#### 2. Controller Validation
```java
@PostMapping
public ResponseEntity<TicketResponse> createTicket(
    @Valid @RequestBody CreateTicketRequest request  // @Valid es crítico
) {
    // Spring valida automáticamente
}
```

#### 3. Custom Exceptions
```java
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
```

---

## Database Schema

### Tablas Principales

#### 1. ticket
```sql
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    position_in_queue INTEGER NOT NULL,
    estimated_wait_minutes INTEGER NOT NULL,
    assigned_advisor_id BIGINT,
    assigned_module_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. advisor
```sql
CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. mensaje
```sql
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) 
        ON DELETE CASCADE
);
```

### Índices para Performance
```sql
-- Búsquedas frecuentes
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);

-- Ordenamiento
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);

-- Scheduler queries
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
```

---

## Configuración y Deployment

### Configuración de Spring

#### application.yml (Principal)
```yaml
spring:
  application:
    name: ticketero-api

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ticketero}
    username: ${DATABASE_USERNAME:dev}
    password: ${DATABASE_PASSWORD:dev123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway maneja el schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Telegram Configuration
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN:}
  api-url: https://api.telegram.org/bot

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# Logging
logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate.SQL: DEBUG
```

### Docker Configuration

#### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    ports:
      - "5432:5432"
    
  api:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      SPRING_PROFILES_ACTIVE: dev
```

### Environment Variables
```bash
# .env file
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123
TELEGRAM_BOT_TOKEN=your_bot_token_here
```

---

## Testing Strategy

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @InjectMocks
    private TicketService ticketService;
    
    @Test
    void shouldCreateTicketSuccessfully() {
        // Given
        CreateTicketRequest request = new CreateTicketRequest(...);
        
        // When
        TicketResponse response = ticketService.createTicket(request);
        
        // Then
        assertThat(response.numero()).startsWith("C");
    }
}
```

### Integration Tests
```java
@SpringBootTest
@Testcontainers
class TicketControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldCreateTicketViaAPI() {
        // Test completo con base de datos real
    }
}
```

---

## Métricas y Monitoreo

### Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Health Checks
- `/actuator/health` - Estado general
- `/actuator/info` - Información de la aplicación
- `/actuator/metrics` - Métricas de rendimiento

---

## Próximos Pasos

### Mejoras Técnicas Pendientes
1. **Implementación completa de TelegramService** - Integración real con Telegram Bot API
2. **Circuit Breakers** - Resilience4j para llamadas externas
3. **Caching** - Redis para consultas frecuentes
4. **Métricas avanzadas** - Micrometer + Prometheus
5. **Documentación API** - OpenAPI/Swagger

### Refactoring Identificado
1. **Exception handling** - Crear excepciones específicas del dominio
2. **Validation groups** - Para diferentes escenarios de validación
3. **Event-driven architecture** - Para notificaciones asíncronas
4. **Audit logging** - Trazabilidad completa de operaciones

---

**Documentación generada automáticamente**  
**Última actualización:** Diciembre 2025  
**Mantenida por:** Equipo de Desarrollo Ticketero Digital