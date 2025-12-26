# Endpoints API - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Base URL:** `http://localhost:8080/api`

---

## 1. Resumen de Endpoints

### 1.1 Estadísticas Generales
- **Total de Endpoints:** 16
- **Endpoints Públicos (Cliente):** 7
- **Endpoints Administrativos:** 9
- **Métodos HTTP:** GET (11), POST (4), PUT (1)

### 1.2 Distribución por Funcionalidad
| Funcionalidad | Cantidad | Endpoints |
|---------------|----------|-----------|
| Gestión de Tickets | 7 | `/api/tickets/*` |
| Administración | 6 | `/api/admin/advisors/*` |
| Dashboard | 1 | `/api/admin/dashboard` |
| Operaciones de Cola | 2 | `/api/admin/tickets/*/complete`, `/api/admin/queues/process` |

---

## 2. Endpoints Públicos (Cliente)

### 2.1 Crear Ticket Digital

**RF-001: Crear Ticket Digital**

```http
POST /api/tickets
Content-Type: application/json
```

**Request Body:**
```json
{
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA"
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "C01",
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA",
  "status": "EN_ESPERA",
  "positionInQueue": 3,
  "estimatedWaitMinutes": 15,
  "assignedAdvisorId": null,
  "assignedAdvisorName": null,
  "assignedModuleNumber": null,
  "createdAt": "2025-12-15T10:30:00Z",
  "updatedAt": "2025-12-15T10:30:00Z"
}
```

**Validaciones:**
- `nationalId`: Obligatorio, 7-20 caracteres
- `telefono`: Opcional, formato internacional válido
- `branchOffice`: Obligatorio, máximo 100 caracteres
- `queueType`: Obligatorio, valores: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA

**Errores:**
- `400 Bad Request`: Validación fallida
- `409 Conflict`: Cliente ya tiene ticket activo

---

### 2.2 Consultar Ticket por ID

**RF-006: Consultar Estado del Ticket**

```http
GET /api/tickets/{id}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "C01",
  "status": "ATENDIENDO",
  "positionInQueue": null,
  "estimatedWaitMinutes": null,
  "assignedAdvisorId": 2,
  "assignedAdvisorName": "Juan Pérez",
  "assignedModuleNumber": 3,
  "createdAt": "2025-12-15T10:30:00Z",
  "updatedAt": "2025-12-15T11:15:00Z"
}
```

**Errores:**
- `404 Not Found`: Ticket no existe

---

### 2.3 Consultar Ticket por Código de Referencia

**RF-006: Consultar Estado del Ticket**

```http
GET /api/tickets/codigo/{codigoReferencia}
```

**Ejemplo:**
```http
GET /api/tickets/codigo/a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6
```

**Response:** Igual que consulta por ID

---

### 2.4 Consultar Ticket por Número

**RF-003: Calcular Posición y Tiempo Estimado**

```http
GET /api/tickets/numero/{numero}
```

**Ejemplo:**
```http
GET /api/tickets/numero/C01
```

**Response 200 OK:**
```json
{
  "numero": "C01",
  "status": "EN_ESPERA",
  "positionInQueue": 5,
  "estimatedWaitMinutes": 25,
  "queueType": "CAJA",
  "assignedAdvisorName": null,
  "assignedModuleNumber": null,
  "lastUpdated": "2025-12-15T10:45:00Z"
}
```

---

### 2.5 Listar Tickets con Filtros

**RF-005: Gestionar Múltiples Colas**

```http
GET /api/tickets?status={status}&queueType={queueType}
```

**Parámetros de Query:**
- `status`: EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO
- `queueType`: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA

**Ejemplos:**
```http
GET /api/tickets?status=EN_ESPERA
GET /api/tickets?queueType=CAJA
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "numero": "C01",
    "status": "EN_ESPERA",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 15,
    "queueType": "CAJA"
  }
]
```

**Errores:**
- `400 Bad Request`: Sin parámetros de filtro

---

### 2.6 Actualizar Estado de Ticket

```http
PUT /api/tickets/{id}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "CANCELADO"
}
```

**Response 200 OK:** Ticket actualizado

---

### 2.7 Consultar Tickets Activos por Cola

**RF-005: Gestionar Múltiples Colas**

```http
GET /api/tickets/queue/{queueType}
```

**Ejemplo:**
```http
GET /api/tickets/queue/PERSONAL_BANKER
```

**Response:** Lista de tickets activos en la cola especificada

---

## 3. Endpoints Administrativos

### 3.1 Dashboard Principal

**RF-007: Panel de Monitoreo para Supervisor**

```http
GET /api/admin/dashboard
```

**Response 200 OK:**
```json
{
  "totalTicketsToday": 156,
  "ticketsInQueue": 12,
  "ticketsBeingServed": 5,
  "ticketsCompleted": 139,
  "averageWaitTime": 18.5,
  "advisors": [
    {
      "id": 1,
      "name": "Juan Pérez",
      "email": "juan.perez@banco.com",
      "status": "BUSY",
      "moduleNumber": 1,
      "assignedTicketsCount": 1,
      "ticketsCompletedToday": 12
    }
  ],
  "ticketsByQueueType": {
    "CAJA": 8,
    "PERSONAL_BANKER": 3,
    "EMPRESAS": 1,
    "GERENCIA": 0
  },
  "ticketsByStatus": {
    "EN_ESPERA": 10,
    "PROXIMO": 2,
    "ATENDIENDO": 5
  }
}
```

**Propósito:** Monitoreo en tiempo real de la operación completa

---

### 3.2 Gestión de Asesores

#### 3.2.1 Listar Todos los Asesores

**RF-007: Panel de Monitoreo para Supervisor**

```http
GET /api/admin/advisors
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Juan Pérez",
    "email": "juan.perez@banco.com",
    "status": "AVAILABLE",
    "moduleNumber": 1,
    "assignedTicketsCount": 0,
    "ticketsCompletedToday": 8
  }
]
```

#### 3.2.2 Consultar Asesor por ID

```http
GET /api/admin/advisors/{id}
```

**Response:** Información detallada del asesor

#### 3.2.3 Filtrar Asesores por Estado

```http
GET /api/admin/advisors/status/{status}
```

**Estados válidos:** AVAILABLE, BUSY, OFFLINE

#### 3.2.4 Listar Asesores Disponibles

**RF-004: Asignar Ticket a Ejecutivo Automáticamente**

```http
GET /api/admin/advisors/available
```

**Response:** Lista de asesores con status AVAILABLE

#### 3.2.5 Cambiar Estado de Asesor

**RF-004: Asignar Ticket a Ejecutivo Automáticamente**

```http
PUT /api/admin/advisors/{id}/status?status={newStatus}
```

**Ejemplo:**
```http
PUT /api/admin/advisors/1/status?status=OFFLINE
```

**Response 200 OK:** Asesor actualizado

**Validaciones:**
- Estado debe ser válido: AVAILABLE, BUSY, OFFLINE
- Asesor debe existir

---

### 3.3 Operaciones de Tickets Administrativas

#### 3.3.1 Completar Ticket

```http
POST /api/admin/tickets/{id}/complete
```

**Propósito:** Marcar ticket como completado manualmente

**Response 200 OK:** Operación exitosa

#### 3.3.2 Cancelar Ticket

```http
POST /api/admin/tickets/{id}/cancel
```

**Propósito:** Cancelar ticket manualmente

**Response 200 OK:** Operación exitosa

---

### 3.4 Procesamiento de Colas

#### 3.4.1 Procesar Colas Manualmente

**RF-004: Asignar Ticket a Ejecutivo Automáticamente**

```http
POST /api/admin/queues/process
```

**Propósito:** Ejecutar manualmente el algoritmo de asignación de tickets

**Response 200 OK:** Procesamiento iniciado

---

## 4. Relación con Requerimientos Funcionales

### 4.1 Matriz RF → Endpoints

| RF | Requerimiento | Endpoints Relacionados |
|----|---------------|------------------------|
| RF-001 | Crear Ticket Digital | `POST /api/tickets` |
| RF-002 | Notificaciones Telegram | *Proceso automatizado interno* |
| RF-003 | Calcular Posición y Tiempo | `GET /api/tickets/numero/{numero}` |
| RF-004 | Asignar Ticket Automáticamente | `PUT /api/admin/advisors/{id}/status`<br>`POST /api/admin/queues/process` |
| RF-005 | Gestionar Múltiples Colas | `GET /api/tickets?queueType={type}`<br>`GET /api/tickets/queue/{queueType}` |
| RF-006 | Consultar Estado Ticket | `GET /api/tickets/{id}`<br>`GET /api/tickets/codigo/{uuid}`<br>`GET /api/tickets/numero/{numero}` |
| RF-007 | Panel de Monitoreo | `GET /api/admin/dashboard`<br>`GET /api/admin/advisors` |
| RF-008 | Auditoría de Eventos | *Información no disponible en código actual* |

### 4.2 Endpoints Faltantes Según Requerimientos

Basado en el análisis de los requerimientos funcionales, los siguientes endpoints están **faltantes** en la implementación actual:

1. **RF-008 - Auditoría:**
   - `GET /api/admin/audit` - Consultar eventos de auditoría
   - `GET /api/admin/audit/stats` - Estadísticas de auditoría
   - `GET /api/admin/audit/export` - Exportar registros

2. **RF-005 - Gestión de Colas:**
   - `GET /api/admin/queues/{type}` - Estado de cola específica
   - `GET /api/admin/queues/{type}/stats` - Estadísticas detalladas
   - `GET /api/admin/queues/summary` - Resumen de todas las colas

---

## 5. Validaciones y Manejo de Errores

### 5.1 Validaciones de Entrada

**CreateTicketRequest:**
- `nationalId`: Obligatorio, 7-20 caracteres
- `telefono`: Opcional, patrón regex `^\\+?[1-9]\\d{1,14}$`
- `branchOffice`: Obligatorio, máximo 100 caracteres
- `queueType`: Obligatorio, enum válido

### 5.2 Códigos de Error HTTP

| Código | Descripción | Casos de Uso |
|--------|-------------|--------------|
| 200 | OK | Operaciones exitosas |
| 201 | Created | Ticket creado exitosamente |
| 400 | Bad Request | Validación fallida, parámetros inválidos |
| 404 | Not Found | Ticket o asesor no encontrado |
| 409 | Conflict | Cliente ya tiene ticket activo |
| 500 | Internal Server Error | Error interno del sistema |

### 5.3 Estructura de Respuesta de Error

```json
{
  "message": "Validation failed",
  "status": 400,
  "errors": [
    "nationalId: National ID is required",
    "queueType: Queue type is required"
  ]
}
```

---

## 6. Reglas de Negocio Implementadas

### 6.1 RN-001: Unicidad de Ticket Activo
- **Endpoint:** `POST /api/tickets`
- **Validación:** Sistema rechaza creación si cliente tiene ticket activo
- **Error:** HTTP 409 Conflict

### 6.2 RN-002: Prioridad de Colas
- **Endpoint:** `POST /api/admin/queues/process`
- **Implementación:** Asignación automática por prioridad (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)

### 6.3 RN-003: Orden FIFO
- **Endpoints:** Consultas de tickets por cola
- **Implementación:** Tickets ordenados por `createdAt`

### 6.4 RN-004: Balanceo de Carga
- **Endpoint:** `GET /api/admin/advisors/available`
- **Implementación:** Selección por menor `assignedTicketsCount`

### 6.5 RN-005 y RN-006: Formato de Número
- **Endpoint:** `POST /api/tickets`
- **Implementación:** Generación automática con prefijos C, P, E, G

### 6.6 RN-010: Cálculo de Tiempo Estimado
- **Endpoint:** `GET /api/tickets/numero/{numero}`
- **Fórmula:** `posición × tiempoPromedioCola`

---

## 7. Consideraciones de Seguridad

### 7.1 Endpoints Públicos vs Administrativos
- **Públicos:** `/api/tickets/*` - Acceso sin autenticación
- **Administrativos:** `/api/admin/*` - **Requieren autenticación** (no implementada en código actual)

### 7.2 Validación de Entrada
- Todas las requests utilizan `@Valid` para validación automática
- Manejo centralizado de errores con `@ControllerAdvice`

### 7.3 Logging de Seguridad
- Todos los endpoints registran accesos con `@Slf4j`
- Información de requests en logs para auditoría

---

## 8. Limitaciones Identificadas

### 8.1 Funcionalidades Faltantes
1. **Autenticación y Autorización:** Endpoints administrativos sin protección
2. **Auditoría Completa:** Sin endpoints para consultar eventos de auditoría
3. **Estadísticas Avanzadas:** Faltan métricas detalladas por cola
4. **Paginación:** Listas sin paginación implementada

### 8.2 Validaciones Pendientes
1. **RN-001:** Validación de ticket activo no implementada completamente
2. **Límites de Rate:** Sin protección contra abuso de API
3. **Validación de Horarios:** Sin restricciones de horario operativo

---

## 9. Recomendaciones

### 9.1 Mejoras Prioritarias
1. **Implementar autenticación** para endpoints administrativos
2. **Agregar endpoints de auditoría** según RF-008
3. **Implementar paginación** en listas de tickets
4. **Agregar validación completa** de RN-001 (ticket activo único)

### 9.2 Optimizaciones
1. **Cache** para consultas frecuentes de posición
2. **WebSockets** para actualizaciones en tiempo real del dashboard
3. **Métricas** con Micrometer para monitoreo
4. **Documentación OpenAPI** para mejor integración

---

**Documento generado automáticamente basado en:**
- Análisis de código Java en `/src/main/java/com/example/ticketero/controller/`
- Requerimientos funcionales en `/docs/REQUERIMIENTOS-FUNCIONALES.md`
- Validaciones implementadas en DTOs y Exception Handlers

**Última actualización:** Diciembre 2025