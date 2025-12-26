# Arquitectura de Software - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Arquitecto:** Arquitecto de Software Senior

---

## 1. Resumen Ejecutivo

Este documento presenta la arquitectura de software del Sistema Ticketero Digital, diseñado para modernizar la experiencia de atención en sucursales bancarias mediante digitalización completa del proceso de tickets, notificaciones automáticas en tiempo real vía Telegram, y asignación inteligente de clientes a ejecutivos.

La arquitectura propuesta se basa en principios de simplicidad (80/20), escalabilidad progresiva, y tecnologías maduras del ecosistema Java/Spring Boot, optimizada para el volumen esperado de 25,000 tickets/día en fase nacional.

## 2. Stack Tecnológico

### 2.1 Backend Framework

**Selección:** Java 21 + Spring Boot 3.2.11

**Justificación:**

#### ¿Por qué Java 21?
- **Virtual Threads (Project Loom):** Manejo eficiente de concurrencia para schedulers que procesan mensajes cada 60 segundos
- **Records:** DTOs inmutables sin boilerplate, perfectos para Request/Response objects
- **Pattern Matching:** Código más limpio y expresivo para manejo de estados de tickets
- **LTS (Long Term Support):** Soporte hasta 2029, crítico para sistemas financieros
- **Ecosistema maduro:** Amplia adopción en aplicaciones empresariales del sector financiero

#### ¿Por qué Spring Boot 3.2.11?
- **Spring Data JPA:** Reducción de 80% de código de acceso a datos con repositories automáticos
- **Spring Scheduling:** @Scheduled para procesamiento asíncrono de mensajes y gestión de colas
- **Bean Validation:** Validación declarativa con @Valid en controllers
- **Actuator:** Endpoints de salud y métricas out-of-the-box para monitoreo
- **Amplia adopción:** 95% de instituciones financieras usan Spring Boot

#### Alternativas Consideradas:
| Tecnología | Pros | Contras | Decisión |
|------------|------|---------|----------|
| Node.js + NestJS | Async nativo, menor footprint | Menos maduro para apps críticas | ❌ No |
| Go + Gin | Performance superior | Ecosistema menos maduro para CRUD | ❌ No |
| .NET Core | Excelente tooling | Licenciamiento, menos adopción | ❌ No |

### 2.2 Base de Datos

**Selección:** PostgreSQL 16

**Justificación:**

#### ¿Por qué PostgreSQL 16?
- **ACID compliant:** Crítico para transacciones financieras y consistencia de datos
- **JSONB:** Flexibilidad para almacenar metadata de mensajes y datos de auditoría
- **Índices avanzados:** B-tree, GiST para queries complejas de posición en cola
- **Row-level locking:** Concurrencia segura para asignación automática de tickets
- **Particionamiento:** Escalabilidad para auditoría (millones de registros históricos)
- **Open source:** Sin costos de licenciamiento

#### Alternativas Consideradas:
| Base de Datos | Pros | Contras | Decisión |
|---------------|------|---------|----------|
| MySQL | Amplia adopción | Menor soporte de JSON | ❌ No |
| MongoDB | Flexible schema | No ACID para múltiples docs | ❌ No |
| Oracle | Features empresariales | Costos prohibitivos | ❌ No |

### 2.3 Migraciones de Base de Datos

**Selección:** Flyway

**Justificación:**

#### ¿Por qué Flyway?
- **Versionamiento automático:** Control de esquema de BD con archivos SQL numerados
- **Rollback seguro:** Capacidad de revertir cambios en producción
- **Integración nativa:** Spring Boot detecta y ejecuta migraciones automáticamente
- **Validación de checksums:** Detecta cambios manuales no autorizados
- **Simplicidad:** Archivos SQL planos (V1__..., V2__...) fáciles de mantener

#### Alternativa:
- **Liquibase:** Más verboso (XML/YAML), overkill para este proyecto

### 2.4 Integración con Telegram

**Selección:** Telegram Bot HTTP API + RestTemplate

**Justificación:**

#### ¿Por qué Telegram Bot API?
- **Canal preferido:** Especificado por el cliente como canal de notificación
- **API HTTP simple:** Bien documentada, fácil integración con Spring Boot
- **Sin costo:** Gratuito vs WhatsApp Business API ($0.005/mensaje)
- **Rate limits generosos:** 30 mensajes/segundo, suficiente para 25,000 tickets/día
- **HTML formatting:** Soporte de mensajes enriquecidos con emojis y formato

#### ¿Por qué RestTemplate (no WebClient)?
- **Simplicidad:** API síncrona más fácil de debuggear y mantener
- **Volumen apropiado:** Suficiente para 0.3 requests/segundo a Telegram
- **Menor curva de aprendizaje:** Equipo familiarizado con API blocking
- **WebClient overkill:** Reactivo innecesario para este volumen

### 2.5 Containerización

**Selección:** Docker + Docker Compose

**Justificación:**

#### ¿Por qué Docker?
- **Paridad dev/prod:** Elimina "funciona en mi máquina", garantiza consistencia
- **Multi-stage builds:** Imagen final optimizada <150MB
- **Aislamiento de dependencias:** Evita conflictos entre versiones
- **Estándar de la industria:** 90% adopción en empresas modernas

#### ¿Por qué Docker Compose?
- **Orquestación simple:** Ideal para desarrollo y staging
- **Definición declarativa:** Servicios (API + PostgreSQL) en un archivo YAML
- **Redes automáticas:** Comunicación entre contenedores sin configuración manual
- **Migración fácil:** Para producción en AWS, migración directa a ECS/Fargate

### 2.6 Build Tool

**Selección:** Maven 3.9+

**Justificación:**

#### ¿Por qué Maven?
- **Convención sobre configuración:** Estructura estándar de proyectos
- **Repositorio central:** 10M+ artifacts disponibles
- **Plugins maduros:** Spring Boot Maven Plugin para packaging
- **Estándar empresarial:** Preferido en instituciones financieras

---

## 3. Diagramas de Arquitectura

### 3.1 Diagrama de Contexto C4

El siguiente diagrama muestra el sistema Ticketero en su contexto, incluyendo actores externos y sistemas con los que interactúa.

```plantuml
@startuml Diagrama de Contexto - Sistema Ticketero
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

title Diagrama de Contexto (C4 Level 1) - Sistema Ticketero

' Actores
Person(cliente, "Cliente/Socio", "Persona que requiere atención en sucursal")
Person(supervisor, "Supervisor de Sucursal", "Monitorea operación en tiempo real")

' Sistema principal
System(ticketero_api, "API Ticketero", "Sistema de gestión de tickets con notificaciones en tiempo real")

' Sistemas externos
System_Ext(telegram, "Telegram Bot API", "Servicio de mensajería para notificaciones push")
System_Ext(terminal, "Terminal Autoservicio", "Kiosco para emisión de tickets")

' Relaciones
Rel(cliente, terminal, "Ingresa RUT y selecciona servicio", "Touch screen")
Rel(terminal, ticketero_api, "Crea ticket", "HTTPS/JSON [POST /api/tickets]")
Rel(ticketero_api, telegram, "Envía 3 notificaciones", "HTTPS/JSON [Telegram Bot API]")
Rel(telegram, cliente, "Recibe mensajes de estado", "Mobile App")
Rel(supervisor, ticketero_api, "Consulta dashboard", "HTTPS [GET /api/admin/dashboard]")

SHOW_LEGEND()

@enduml
```

**Descripción de Componentes:**

- **Cliente/Socio:** Persona que requiere atención bancaria y usa el sistema para obtener un ticket digital
- **Supervisor de Sucursal:** Usuario administrativo que monitorea la operación en tiempo real
- **API Ticketero:** Sistema principal que gestiona tickets, colas, asignaciones y notificaciones
- **Telegram Bot API:** Servicio externo para envío de notificaciones push a clientes
- **Terminal Autoservicio:** Kiosco físico donde los clientes crean sus tickets

**Flujos Principales:**
1. Cliente interactúa con terminal para crear ticket
2. Terminal envía datos al API Ticketero
3. API programa y envía notificaciones vía Telegram
4. Cliente recibe mensajes en su dispositivo móvil
5. Supervisor consulta dashboard para monitoreo

**Archivo fuente:** `docs/diagrams/01-context-diagram.puml`

**Nota:** Para visualizar el diagrama, usar plugins de PlantUML en IDE o http://www.plantuml.com/plantuml/

### 3.2 Diagrama de Secuencia

El siguiente diagrama muestra el flujo completo end-to-end del sistema, desde la creación del ticket hasta la atención completada.

```plantuml
@startuml Secuencia Completa - Sistema Ticketero

title Flujo End-to-End: Creación de Ticket y Asignación a Asesor

actor Cliente
participant "Terminal" as Terminal
participant "TicketController" as Controller
participant "TicketService" as Service
participant "TelegramService" as Telegram
participant "PostgreSQL" as DB
participant "MessageScheduler" as Scheduler
participant "QueueProcessor" as QueueProc
participant "Advisor" as Asesor

== Fase 1: Creación de Ticket ==

Cliente -> Terminal: Ingresa RUT, teléfono, selecciona PERSONAL_BANKER
Terminal -> Controller: POST /api/tickets
Controller -> Service: crearTicket(request)
Service -> DB: INSERT INTO ticket (numero: P01, status: EN_ESPERA, positionInQueue: 5)

alt teléfono informado (RN-014)
    Service -> DB: INSERT INTO mensaje (3 mensajes programados)
else teléfono null/vacío
    note over Service: NO se programan mensajes Telegram
end

Service --> Controller: TicketResponse(numero: P01, positionInQueue: 5, estimatedWait: 75min)
Controller --> Terminal: HTTP 201 + JSON
Terminal --> Cliente: Muestra ticket P01

== Fase 2: Envío de Mensaje 1 (Inmediato) ==

note over Scheduler: Ejecuta cada 60 segundos

alt existen mensajes pendientes
    Scheduler -> DB: SELECT mensajes WHERE estadoEnvio=PENDIENTE AND fechaProgramada <= NOW
    DB --> Scheduler: [Mensaje 1: totem_ticket_creado]
    
    alt envío exitoso
        Scheduler -> Telegram: POST sendMessage (chatId, "✅ Ticket P01, posición #5, 75min")
        Telegram --> Scheduler: {ok: true, message_id: 123}
        Scheduler -> DB: UPDATE mensaje SET estadoEnvio=ENVIADO, telegramMessageId=123
        Scheduler --> Cliente: Notificación en Telegram
    else fallo de red (RN-007/RN-008)
        Scheduler -> Telegram: POST sendMessage
        Telegram --> Scheduler: {error: timeout}
        note over Scheduler: estadoEnvio=PENDIENTE\nReintentos: 30s, 60s, 120s (backoff)
    end
else no existen mensajes pendientes
    note over Scheduler: No hay mensajes programados\n(ticket creado sin teléfono – RN-014)
end

== Fase 3: Progreso de Cola (cuando posición <= 3) ==

note over QueueProc: Ejecuta cada 5 segundos\nRecalcula posición según FIFO por createdAt\ny considera solo tickets EN_ESPERA de la misma cola
QueueProc -> DB: Recalcula posiciones de todos los tickets EN_ESPERA
QueueProc -> DB: UPDATE ticket SET positionInQueue = (nueva posición)
QueueProc -> DB: SELECT tickets WHERE positionInQueue <= 3 AND status = EN_ESPERA
DB --> QueueProc: [Ticket P01, posición: 3]
QueueProc -> DB: UPDATE ticket SET status = PROXIMO

note over Scheduler: Detecta Mensaje 2 programado
Scheduler -> Telegram: POST sendMessage ("⏰ Pronto será tu turno P01")
Telegram --> Scheduler: {ok: true}
Scheduler --> Cliente: Notificación Pre-aviso

== Fase 4: Asignación Automática a Asesor ==

QueueProc -> DB: SELECT advisors WHERE status=AVAILABLE ORDER BY assignedTicketsCount LIMIT 1
DB --> QueueProc: [Advisor: María González, moduleNumber: 3]
QueueProc -> DB: UPDATE ticket SET assignedAdvisor=María, assignedModuleNumber=3, status=ATENDIENDO
QueueProc -> DB: UPDATE advisor SET status=BUSY, assignedTicketsCount=assignedTicketsCount+1

note over Scheduler: Detecta Mensaje 3 programado
Scheduler -> Telegram: POST sendMessage ("🔔 ES TU TURNO P01! Módulo 3, Asesora: María González")
Telegram --> Scheduler: {ok: true}
Scheduler --> Cliente: Notificación Turno Activo

QueueProc -> Asesor: Notifica en terminal del asesor
Asesor --> Cliente: Atiende al cliente en módulo 3

== Fase 5: Completar Atención ==

Asesor -> Controller: PUT /api/admin/advisors/1/complete-ticket
Controller -> Service: completarTicket(ticketId)
Service -> DB: UPDATE ticket SET status=COMPLETADO
Service -> DB: UPDATE advisor SET status=AVAILABLE, assignedTicketsCount=assignedTicketsCount-1
Service -> DB: INSERT INTO auditoria (evento: TICKET_COMPLETADO)
Service --> Controller: {success: true}

@enduml
```

**Descripción de las Fases:**

- **Fase 1:** Cliente crea ticket en terminal, sistema calcula posición real
- **Fase 2:** Scheduler envía confirmación inmediata vía Telegram
- **Fase 3:** Sistema monitorea progreso, envía pre-aviso cuando posición ≤ 3
- **Fase 4:** Asignación automática a asesor disponible, envía notificación final
- **Fase 5:** Asesor completa atención, sistema libera recursos

**Archivo fuente:** `docs/diagrams/02-sequence-diagram.puml`

### 3.3 Modelo de Datos ER

El siguiente diagrama muestra el modelo entidad-relación de la base de datos PostgreSQL.

```plantuml
@startuml Modelo de Datos - Sistema Ticketero

!define Table(name,desc) class name as "desc" << (T,#FFAAAA) >>
!define primary_key(x) <b>PK: x</b>
!define foreign_key(x) <color:red>FK: x</color>
!define unique(x) <color:green>UQ: x</color>

hide methods
hide stereotypes

' Entidades

Table(ticket, "ticket") {
  primary_key(id: BIGSERIAL)
  unique(codigo_referencia: UUID)
  unique(numero: VARCHAR(10))
  national_id: VARCHAR(20)
  telefono: VARCHAR(20)
  branch_office: VARCHAR(100)
  queue_type: VARCHAR(20)
  status: VARCHAR(20)
  position_in_queue: INTEGER
  estimated_wait_minutes: INTEGER
  created_at: TIMESTAMP
  updated_at: TIMESTAMP
  foreign_key(assigned_advisor_id: BIGINT)
  assigned_module_number: INTEGER
}

Table(mensaje, "mensaje") {
  primary_key(id: BIGSERIAL)
  foreign_key(ticket_id: BIGINT)
  plantilla: VARCHAR(50)
  estado_envio: VARCHAR(20)
  fecha_programada: TIMESTAMP
  fecha_envio: TIMESTAMP
  telegram_message_id: VARCHAR(50)
  intentos: INTEGER
  created_at: TIMESTAMP
}

Table(advisor, "advisor") {
  primary_key(id: BIGSERIAL)
  name: VARCHAR(100)
  email: VARCHAR(100)
  status: VARCHAR(20)
  module_number: INTEGER
  assigned_tickets_count: INTEGER
  created_at: TIMESTAMP
  updated_at: TIMESTAMP
}

' Relaciones

ticket "1" -- "0..*" mensaje : "tiene mensajes programados"
advisor "1" -- "0..*" ticket : "atiende tickets"

' Notas

note right of ticket
  **Estados posibles:**
  - EN_ESPERA
  - PROXIMO
  - ATENDIENDO
  - COMPLETADO
  - CANCELADO
  - NO_ATENDIDO
  
  **Tipos de cola:**
  - CAJA
  - PERSONAL_BANKER
  - EMPRESAS
  - GERENCIA
  
  **Restricción:** un advisor puede tener
  como máximo 1 ticket en estado
  ATENDIENDO simultáneamente.
end note

note right of mensaje
  **Plantillas:**
  - totem_ticket_creado
  - totem_proximo_turno
  - totem_es_tu_turno
  
  **Estados:**
  - PENDIENTE
  - ENVIADO
  - FALLIDO
  
  **telegram_message_id** es identificador
  externo devuelto por Telegram.
end note

note right of advisor
  **Estados:**
  - AVAILABLE
  - BUSY
  - OFFLINE
  
  **Módulos:** 1-5
end note

@enduml
```

**Descripción de las Relaciones:**

- **ticket ← mensaje (1:N):** Un ticket puede tener múltiples mensajes programados (confirmación, pre-aviso, turno activo)
- **advisor ← ticket (1:N):** Un asesor puede atender múltiples tickets (pero solo 1 a la vez en estado ATENDIENDO)

**Índices Importantes:**

- `ticket.codigo_referencia` (UNIQUE): Búsqueda rápida por UUID
- `ticket.numero` (UNIQUE): Búsqueda por número de ticket
- `ticket.national_id`: Validación de ticket activo por cliente
- `ticket.status`: Filtrado de tickets por estado
- `Índice compuesto (estado_envio, fecha_programada)`: Query del scheduler
- `advisor.status`: Selección de asesores disponibles

**Archivo fuente:** `docs/diagrams/03-er-diagram.puml`

## 4. Arquitectura en Capas

### 4.1 Diagrama de Capas

```
┌─────────────────────────────────────────────────────────┐
│ CAPA DE PRESENTACIÓN (Controllers)                     │
│ - TicketController                                      │
│ - AdminController                                       │
│ - Recibe HTTP requests                                  │
│ - Valida con @Valid                                     │
│ - Retorna ResponseEntity<DTO>                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ CAPA DE NEGOCIO (Services)                              │
│ - TicketService                                         │
│ - TelegramService                                       │
│ - QueueManagementService                                │
│ - AdvisorService                                        │
│ - Lógica de negocio                                     │
│ - Transacciones (@Transactional)                       │
│ - Orquestación de operaciones                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ CAPA DE DATOS (Repositories)                            │
│ - TicketRepository extends JpaRepository                │
│ - MensajeRepository                                     │
│ - AdvisorRepository                                     │
│ - Queries custom con @Query                             │
│ - Spring Data JPA                                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ BASE DE DATOS (PostgreSQL)                              │
│ - ticket (tabla principal)                              │
│ - mensaje (mensajes programados)                        │
│ - advisor (asesores)                                    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ CAPA ASÍNCRONA (Schedulers)                             │
│ - MessageScheduler (@Scheduled fixedRate=60s)           │
│ - QueueProcessorScheduler (@Scheduled fixedRate=5s)     │
│ - Procesamiento en background                           │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Responsabilidades por Capa

#### Controllers (Capa de Presentación)
**Responsabilidad:** Manejar HTTP requests/responses  
**Prohibido:** Lógica de negocio, acceso directo a DB

```java
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    
    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(@Valid @RequestBody TicketRequest request) {
        // 1. Validar (automático con @Valid)
        // 2. Delegar a service
        // 3. Retornar response
    }
}
```

#### Services (Capa de Negocio)
**Responsabilidad:** Lógica de negocio, transacciones, orquestación  
**Prohibido:** Lógica de presentación (HTTP codes), SQL directo

```java
@Service
@Transactional
public class TicketService {
    
    public TicketResponse crearTicket(TicketRequest request) {
        // 1. Validar reglas de negocio (RN-001: único ticket activo)
        // 2. Generar número de ticket
        // 3. Calcular posición (RN-010)
        // 4. Persistir ticket
        // 5. Programar mensajes (solo si teléfono informado – RN-014)
        // 6. Registrar auditoría (RN-011)
        // 7. Retornar response
    }
}
```

#### Repositories (Capa de Datos)
**Responsabilidad:** Acceso a datos, queries  
**Prohibido:** Lógica de negocio

```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    @Query("SELECT t FROM Ticket t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Ticket> findByStatusOrderByCreatedAtAsc(@Param("status") String status);
}
```

#### Schedulers (Capa Asíncrona)
**Responsabilidad:** Procesamiento en background  
**Prohibido:** HTTP requests directos de clientes

```java
@Component
public class MessageScheduler {
    
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void procesarMensajesPendientes() {
        // 1. Buscar mensajes con estado=PENDIENTE y fechaProgramada <= NOW
        // 2. Enviar vía TelegramService
        // 3. Actualizar estado a ENVIADO/FALLIDO
    }
}
```

## 5. Componentes Principales

### 5.1 TicketController

**Responsabilidad:** Exponer API REST para gestión de tickets

**Endpoints:**
- `POST /api/tickets` - Crear ticket (RF-001)
- `GET /api/tickets/{uuid}` - Obtener ticket (RF-006)
- `GET /api/tickets/{numero}/position` - Consultar posición (RF-003)

**Dependencias:**
- TicketService

**Validaciones:**
- @Valid en TicketRequest (Bean Validation)
- Códigos HTTP apropiados (201 Created, 409 Conflict, 400 Bad Request)

### 5.2 AdminController

**Responsabilidad:** Panel administrativo para supervisores

**Endpoints:**
- `GET /api/admin/dashboard` - Dashboard completo (RF-007)
- `GET /api/admin/queues/{type}` - Estado de cola (RF-005)
- `GET /api/admin/advisors` - Lista de asesores (RF-007)
- `PUT /api/admin/advisors/{id}/status` - Cambiar estado asesor

**Dependencias:**
- QueueManagementService
- AdvisorService
- TicketRepository

**Actualización:** Dashboard se actualiza cada 5 segundos (RNF-002)

### 5.3 TicketService

**Responsabilidad:** Lógica de negocio para tickets

**Métodos Principales:**
- `crearTicket(TicketRequest) → TicketResponse`
  * Valida RN-001 (único ticket activo)
  * Genera número según RN-005, RN-006
  * Calcula posición y tiempo (RN-010)
  * Programa mensajes (solo si teléfono informado – RN-014)
  * Registra auditoría (RN-011)

- `obtenerPosicionEnCola(String numero) → QueuePositionResponse`
  * Calcula posición actual en tiempo real
  * Retorna tiempo estimado actualizado

**Dependencias:**
- TicketRepository
- MensajeRepository
- TelegramService (para programar mensajes)

**Transacciones:** @Transactional para operaciones de escritura

### 5.4 TelegramService

**Responsabilidad:** Integración con Telegram Bot API

**Métodos Principales:**
- `enviarMensaje(String chatId, String texto) → String messageId`
  * POST a https://api.telegram.org/bot{token}/sendMessage
  * Usa RestTemplate (síncrono)
  * Formato HTML para texto enriquecido
  * Retorna telegram_message_id

- `obtenerTextoMensaje(String plantilla, String numeroTicket) → String`
  * Genera texto según plantilla (totem_ticket_creado, etc.)
  * Usa emojis (✅, ⏰, 🔔)

**Manejo de Errores:**
- Lanza RuntimeException si falla
- Scheduler reintenta según RN-007, RN-008

### 5.5 QueueManagementService

**Responsabilidad:** Gestión de colas y asignación automática

**Métodos Principales:**
- `asignarSiguienteTicket() → void`
  * Ejecutado por QueueProcessorScheduler cada 5s
  * Selecciona asesor AVAILABLE con menor carga (RN-004)
  * Prioriza colas según RN-002 (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
  * Dentro de cola: orden FIFO (RN-003)
  * Actualiza estado ticket a ATENDIENDO
  * Actualiza estado asesor a BUSY

- `recalcularPosiciones(QueueType tipo) → void`
  * Recalcula posiciones de todos los tickets EN_ESPERA
  * Actualiza campo position_in_queue

**Dependencias:**
- TicketRepository
- AdvisorRepository

### 5.6 AdvisorService

**Responsabilidad:** Gestión de asesores y módulos

**Métodos Principales:**
- `obtenerAsesoresDisponibles() → List<AdvisorResponse>`
- `cambiarEstadoAsesor(Long id, AdvisorStatus status) → void`
- `completarTicket(Long advisorId) → void`

**Dependencias:**
- AdvisorRepository
- TicketRepository

### 5.7 MessageScheduler

**Responsabilidad:** Envío asíncrono de mensajes programados

**Configuración:**
- @Scheduled(fixedRate = 60000) // Cada 60 segundos
- @EnableScheduling en clase principal

**Lógica:**
1. Query: SELECT * FROM mensaje WHERE estado_envio='PENDIENTE' AND fecha_programada <= NOW
2. Para cada mensaje:
   - TelegramService.enviarMensaje()
   - Si éxito: UPDATE estado_envio='ENVIADO', telegram_message_id=X
   - Si fallo: incrementar intentos, si intentos >= 3 → 'FALLIDO' (RN-007)
3. Reintentos con backoff: 30s, 60s, 120s (RN-008)

**Manejo de Errores:**
- Try-catch por mensaje (un fallo no detiene el scheduler)
- Logging detallado para debugging

### 5.8 QueueProcessorScheduler

**Responsabilidad:** Procesamiento automático de colas

**Configuración:**
- @Scheduled(fixedRate = 5000) // Cada 5 segundos

**Lógica:**
1. Recalcular posiciones de todos los tickets EN_ESPERA
2. Identificar tickets con posición <= 3 → UPDATE status='PROXIMO' (RN-012)
3. Buscar asesores AVAILABLE
4. Si hay asesor disponible:
   - QueueManagementService.asignarSiguienteTicket()
5. Registrar auditoría de asignaciones

## 6. Decisiones Arquitectónicas (ADRs)

### ADR-001: No usar Circuit Breakers (Resilience4j)

**Contexto:** Telegram Bot API es un servicio externo que podría fallar.

**Decisión:** NO implementar Circuit Breaker en esta fase.

**Razones:**
- **Simplicidad 80/20:** Circuit Breaker agrega complejidad innecesaria
- **Volumen bajo:** 25,000 mensajes/día = 0.3 msg/segundo (no crítico)
- **Telegram tiene 99.9% uptime**
- **Reintentos simples** (RN-007, RN-008) son suficientes
- **Si Telegram falla,** los mensajes quedan PENDIENTES y se reintenta

**Consecuencias:**
- ✅ Código más simple y mantenible
- ✅ Menor curva de aprendizaje
- ❌ Sin protección contra cascading failures (aceptable para este volumen)

**Futuro:**
- Fase 2 (50+ sucursales): reevaluar Resilience4j

---

### ADR-002: RestTemplate en lugar de WebClient

**Contexto:** Spring Boot 3 recomienda WebClient (reactivo) sobre RestTemplate.

**Decisión:** Usar RestTemplate (blocking I/O).

**Razones:**
- **Simplicidad:** API síncrona más fácil de debuggear
- **Volumen bajo:** 0.3 requests/segundo a Telegram
- **WebClient requiere Project Reactor** (curva de aprendizaje)
- **Para este volumen, blocking I/O es suficiente**

**Consecuencias:**
- ✅ Código más simple
- ✅ Stack trace más fácil de leer
- ❌ Menor throughput (no relevante para este caso)

**Futuro:**
- Si volumen supera 10 req/segundo → migrar a WebClient

---

### ADR-003: Scheduler en lugar de Queue (RabbitMQ/Kafka)

**Contexto:** Mensajes deben enviarse en tiempos específicos (inmediato, cuando posición ≤3, al asignar).

**Decisión:** Usar @Scheduled + tabla mensaje en PostgreSQL.

**Razones:**
- **Simplicidad:** no requiere infraestructura adicional (RabbitMQ/Kafka)
- **Volumen bajo:** 25,000 tickets/día × 3 mensajes = 75,000 mensajes/día = 0.9 msg/segundo
- **@Scheduled cada 60s es suficiente** para este throughput
- **PostgreSQL como "queue" es confiable** (ACID)

**Consecuencias:**
- ✅ Infraestructura simple (solo PostgreSQL + API)
- ✅ Sin complejidad de RabbitMQ
- ❌ Polling cada 60s (no tiempo real extremo, aceptable)

**Futuro:**
- Fase Nacional (500,000+ mensajes/día): migrar a RabbitMQ

---

### ADR-004: Flyway para Migraciones

**Decisión:** Usar Flyway en lugar de Liquibase o migraciones manuales.

**Razones:**
- **SQL plano** (fácil de leer y mantener)
- **Versionamiento automático**
- **Rollback seguro**
- **Integración nativa con Spring Boot**

**Consecuencias:**
- ✅ Esquema versionado y auditable
- ✅ Despliegues reproducibles

---

### ADR-005: Bean Validation (@Valid) en DTOs

**Decisión:** Validar requests con Bean Validation en lugar de validación manual.

**Razones:**
- **Declarativo:** @NotBlank, @Pattern directamente en DTOs
- **Spring lo valida automáticamente** con @Valid
- **Mensajes de error estandarizados**

**Ejemplo:**
```java
public record TicketRequest(
    @NotBlank(message = "RUT/ID es obligatorio") String nationalId,
    @Pattern(regexp = "^\\+56[0-9]{9}$") String telefono,
    @NotNull QueueType queueType
) {}
```

**Consecuencias:**
- ✅ Validación consistente
- ✅ Menos código boilerplate
- ✅ Mensajes de error automáticos

## 7. Configuración y Deployment

### 7.1 Variables de Entorno

| Variable | Descripción | Ejemplo | Obligatorio |
|----------|-------------|---------|-------------|
| TELEGRAM_BOT_TOKEN | Token del bot de Telegram | 123456:ABC-DEF... | Sí |
| DATABASE_URL | JDBC URL de PostgreSQL | jdbc:postgresql://db:5432/... | Sí |
| DATABASE_USERNAME | Usuario de base de datos | ticketero_user | Sí |
| DATABASE_PASSWORD | Password de base de datos | *** | Sí |
| SPRING_PROFILES_ACTIVE | Profile activo (dev/prod) | prod | No |

### 7.2 Docker Compose (Desarrollo)

```yaml
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ticketero
      - DATABASE_USERNAME=dev
      - DATABASE_PASSWORD=dev123
    depends_on:
      - postgres

  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=ticketero
      - POSTGRES_USER=dev
      - POSTGRES_PASSWORD=dev123
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### 7.3 Application Properties

```yaml
spring:
  application:
    name: ticketero-api
    
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    
  jpa:
    hibernate:
      ddl-auto: validate # Flyway maneja el schema
    show-sql: false
    properties:
      hibernate.format_sql: true
    
  flyway:
    enabled: true
    baseline-on-migrate: true

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot

logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
```

## 8. Checklist de Completitud

### 8.1 Contenido
- ✅ Stack Tecnológico (6 tecnologías justificadas)
- ✅ Diagrama C4 (renderizable en PlantUML)
- ✅ Diagrama de Secuencia (5 fases documentadas)
- ✅ Modelo ER (3 tablas, 2 relaciones)
- ✅ Arquitectura en Capas (5 capas)
- ✅ 8 Componentes documentados
- ✅ 5 ADRs con formato estándar
- ✅ Configuración completa

### 8.2 Diagramas
- ✅ 3 archivos .puml creados en docs/diagrams/
- ✅ 3 diagramas embebidos en documento
- ✅ Todos renderizables en PlantUML

### 8.3 Calidad
- ✅ Justificaciones técnicas sólidas
- ✅ Decisiones alineadas con requerimientos
- ✅ Ejemplos de código incluidos
- ✅ Formato profesional y consistente

## 9. Limitaciones Conocidas

### 9.1 Trade-offs Aceptados
- **Polling cada 60s:** No tiempo real extremo, pero suficiente para el volumen
- **RestTemplate:** Blocking I/O, pero apropiado para 0.3 req/s
- **Sin Circuit Breaker:** Simplicidad sobre resiliencia avanzada
- **PostgreSQL como queue:** Simple pero no optimizado para alta concurrencia

### 9.2 Escalabilidad
- **Fase Actual:** 25,000 tickets/día (1 sucursal)
- **Límite estimado:** 100,000 tickets/día sin cambios arquitectónicos
- **Migración futura:** RabbitMQ, WebClient, Circuit Breakers

## 10. Roadmap Técnico

### 10.1 Fase 2 (5 sucursales)
- Migrar a WebClient para mejor throughput
- Implementar Circuit Breaker (Resilience4j)
- Optimizar queries con índices adicionales

### 10.2 Fase 3 (50+ sucursales)
- Migrar a RabbitMQ para mensajería
- Implementar cache distribuido (Redis)
- Considerar microservicios por dominio

---

**DOCUMENTO DE ARQUITECTURA COMPLETADO**

**Estadísticas finales:**
- 6 tecnologías justificadas
- 3 diagramas PlantUML renderizables
- 8 componentes documentados
- 5 ADRs con formato estándar
- Configuración completa para desarrollo y producción

**Este documento está listo para:**
- Validación por stakeholders
- Entrada para implementación (PROMPT 3)
- Revisión técnica por equipo de desarrollo
- Aprobación por arquitectos senior
