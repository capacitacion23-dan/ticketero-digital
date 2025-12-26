# Requerimientos del Sistema - Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2025

---

## 1. Resumen Ejecutivo

### 1.1 Descripción del Proyecto

El Sistema Ticketero Digital es una solución integral que moderniza la experiencia de atención en sucursales bancarias mediante la digitalización completa del proceso de tickets, notificaciones automáticas en tiempo real vía Telegram, y un panel de monitoreo para supervisión operacional.

### 1.2 Problemática Actual

Las instituciones financieras enfrentan desafíos significativos en la atención presencial:
- Los clientes no tienen visibilidad de tiempos de espera
- Deben permanecer físicamente en sucursal sin poder realizar otras actividades
- Existe incertidumbre sobre el progreso de su turno
- Falta de trazabilidad para análisis y mejora continua

### 1.3 Solución Propuesta

Sistema digital que permite:
- **Digitalización del proceso de tickets** con códigos únicos
- **Notificaciones automáticas en tiempo real** vía Telegram
- **Movilidad del cliente** durante la espera
- **Asignación automática** de clientes a ejecutivos disponibles
- **Panel de monitoreo** para supervisión operacional

## 2. Resumen de Requerimientos de Negocio

### 2.1 Beneficios Esperados

| Métrica | Situación Actual | Meta |
|---------|------------------|------|
| NPS (Net Promoter Score) | 45 puntos | 65 puntos |
| Tasa de abandono de cola | 15% | 5% |
| Tickets atendidos por ejecutivo | Baseline | +20% |
| Trazabilidad | 0% | 100% |

### 2.2 Fases de Implementación

**Fase Piloto:**
- 500-800 tickets/día
- 1 sucursal
- Validación de concepto

**Fase Expansión:**
- 2,500-3,000 tickets/día
- 5 sucursales
- Optimización operacional

**Fase Nacional:**
- 25,000+ tickets/día
- 50+ sucursales
- Escalamiento completo

### 2.3 Flujo del Proceso

1. **Emisión de Ticket:** Cliente ingresa RUT/ID, selecciona tipo de atención, recibe ticket digital
2. **Notificación de Progreso:** Sistema monitorea cola y envía pre-aviso cuando quedan 3 personas
3. **Asignación y Atención:** Sistema asigna automáticamente a ejecutivo disponible
4. **Supervisión:** Dashboard en tiempo real para monitoreo operacional

## 3. Resumen de Requerimientos Funcionales

### 3.1 Requerimientos Principales

| ID | Requerimiento | Prioridad | Descripción |
|----|---------------|-----------|-------------|
| **RF-001** | Crear Ticket Digital | Alta | Generación de ticket con número único, cálculo de posición y tiempo estimado |
| **RF-002** | Notificaciones Automáticas | Alta | Envío de 3 mensajes vía Telegram: confirmación, pre-aviso y turno activo |
| **RF-003** | Calcular Posición y Tiempo | Alta | Cálculo en tiempo real de posición en cola y tiempo estimado de espera |
| **RF-004** | Asignación Automática | Alta | Asignación inteligente considerando prioridad, balanceo de carga y orden FIFO |
| **RF-005** | Gestión de Múltiples Colas | Alta | Manejo de 4 tipos de cola con diferentes características operacionales |
| **RF-006** | Consultar Estado del Ticket | Alta | Consulta del estado actual del ticket por parte del cliente |
| **RF-007** | Panel de Monitoreo | Alta | Dashboard en tiempo real para supervisión operacional |
| **RF-008** | Auditoría de Eventos | Alta | Registro inmutable de todos los eventos críticos del sistema |

### 3.2 Tipos de Cola

| Cola | Tiempo Promedio | Prioridad | Prefijo | Descripción |
|------|-----------------|-----------|---------|-------------|
| **CAJA** | 5 minutos | 1 (baja) | C | Transacciones básicas, depósitos, retiros |
| **PERSONAL_BANKER** | 15 minutos | 2 (media) | P | Productos financieros, créditos, inversiones |
| **EMPRESAS** | 20 minutos | 3 (media-alta) | E | Clientes corporativos, cuentas empresariales |
| **GERENCIA** | 30 minutos | 4 (máxima) | G | Casos especiales, reclamos, situaciones complejas |

### 3.3 Estados del Sistema

**Estados de Ticket:**
- EN_ESPERA: Esperando asignación a asesor
- PROXIMO: Próximo a ser atendido (posición ≤ 3)
- ATENDIENDO: Siendo atendido por un asesor
- COMPLETADO: Atención finalizada exitosamente
- CANCELADO: Cancelado por cliente o sistema
- NO_ATENDIDO: Cliente no se presentó cuando fue llamado

**Estados de Asesor:**
- AVAILABLE: Disponible para recibir asignaciones
- BUSY: Atendiendo un cliente
- OFFLINE: No disponible (almuerzo, capacitación, etc.)

### 3.4 Plantillas de Mensajes

**Mensaje 1 - Confirmación:**
```
✅ Ticket Creado
Tu número de turno: {numero}
Posición en cola: #{posicion}
Tiempo estimado: {tiempo} minutos
Te notificaremos cuando estés próximo.
```

**Mensaje 2 - Pre-aviso:**
```
⏰ ¡Pronto será tu turno!
Turno: {numero}
Faltan aproximadamente 3 turnos.
Por favor, acércate a la sucursal.
```

**Mensaje 3 - Turno Activo:**
```
🔔 ¡ES TU TURNO {numero}!
Dirígete al módulo: {modulo}
Asesor: {nombreAsesor}
```

## 4. Alcance del Sistema

### 4.1 Funcionalidades Incluidas

✅ **Gestión de Tickets Digitales**
- Creación con RUT/ID y tipo de atención
- Numeración automática con prefijos por cola
- Cálculo de posición y tiempo estimado

✅ **Sistema de Notificaciones**
- Integración con Telegram Bot API
- 3 mensajes automáticos por ticket
- Reintentos con backoff exponencial

✅ **Asignación Inteligente**
- Balanceo de carga entre asesores
- Priorización por tipo de cola
- Orden FIFO dentro de cada cola

✅ **Panel de Supervisión**
- Dashboard en tiempo real
- Estadísticas por cola y asesor
- Sistema de alertas automáticas

✅ **Auditoría y Trazabilidad**
- Registro de todos los eventos críticos
- Información inmutable para análisis
- Exportación de datos para reportes

### 4.2 Funcionalidades Excluidas

❌ **Integración con Core Bancario**
- No se conecta con sistemas transaccionales
- No valida productos o saldos del cliente

❌ **Gestión de Usuarios y Roles**
- No incluye sistema de autenticación complejo
- Acceso básico para supervisores

❌ **Reportes Avanzados**
- No incluye business intelligence
- Reportes básicos únicamente

❌ **Múltiples Canales de Notificación**
- Solo Telegram, no SMS ni email
- No push notifications móviles

### 4.3 Integraciones

**Incluidas:**
- Telegram Bot API para notificaciones
- Base de datos PostgreSQL
- API REST para consultas

**No Incluidas:**
- Core bancario
- Sistemas de CRM
- Plataformas de BI

## 5. Supuestos y Restricciones

### 5.1 Supuestos del Proyecto

**Supuestos Técnicos:**
- Los clientes tienen acceso a Telegram
- Conectividad a internet estable en sucursales
- Infraestructura de servidores disponible
- Base de datos PostgreSQL operativa

**Supuestos Operacionales:**
- Asesores utilizarán terminales para cambiar estado
- Supervisor monitoreará dashboard activamente
- Clientes proporcionarán teléfono voluntariamente
- Horario de atención definido (8:00-18:00)

**Supuestos de Negocio:**
- Adopción gradual por parte de los clientes
- Capacitación básica a personal de sucursal
- Soporte técnico disponible durante implementación
- Métricas de éxito medibles y alcanzables

### 5.2 Restricciones del Sistema

**Restricciones Técnicas:**
- Máximo 99 tickets por cola por día (numeración 01-99)
- Actualización de dashboard cada 5 segundos
- Retención de auditoría mínima de 1 año
- Soporte únicamente para Telegram (no otros mensajeros)

**Restricciones Operacionales:**
- Horario de operación: 8:00 AM - 6:00 PM
- Máximo 5 módulos de atención por sucursal
- Un cliente solo puede tener 1 ticket activo
- Teléfono es dato opcional (sin notificaciones si no se proporciona)

**Restricciones de Performance:**
- Creación de ticket: máximo 3 segundos
- Envío de notificaciones: máximo 5 segundos
- Cálculo de posición: máximo 1 segundo
- Disponibilidad: 99.5% en horario de atención

**Restricciones de Seguridad:**
- Cumplimiento de ley de protección de datos personales
- Encriptación de datos sensibles (teléfonos, RUT)
- Acceso controlado al panel administrativo
- Logs de auditoría de todos los accesos

### 5.3 Dependencias Externas

**Dependencias Críticas:**
- Telegram Bot API disponible y operativo
- Conectividad a internet en sucursales
- Infraestructura de servidores y base de datos
- Terminales de autoservicio funcionales

**Dependencias de Soporte:**
- Equipo de desarrollo para mantenimiento
- Personal de soporte técnico
- Capacitación a usuarios finales
- Documentación técnica actualizada

## 6. Criterios de Éxito

### 6.1 Métricas de Adopción

- **Tasa de uso:** >70% de clientes utilizan sistema digital vs. tickets físicos
- **Satisfacción:** NPS objetivo de 65 puntos
- **Abandono:** Reducción de tasa de abandono a <5%
- **Eficiencia:** +20% tickets atendidos por ejecutivo

### 6.2 Métricas Técnicas

- **Disponibilidad:** 99.5% uptime durante horario de atención
- **Performance:** 95% de operaciones dentro de SLA de tiempo
- **Confiabilidad:** 99.9% de mensajes entregados exitosamente
- **Trazabilidad:** 100% de eventos críticos auditados

### 6.3 Métricas Operacionales

- **Tiempo de espera:** Reducción de 30% en tiempo promedio
- **Visibilidad:** 100% de clientes conocen su posición en cola
- **Movilidad:** 80% de clientes salen de sucursal durante espera
- **Supervisión:** Dashboard utilizado activamente por supervisores

## 7. Riesgos y Mitigaciones

### 7.1 Riesgos Técnicos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Falla de Telegram API | Media | Alto | Reintentos automáticos, alertas de monitoreo |
| Sobrecarga de base de datos | Baja | Alto | Optimización de queries, índices apropiados |
| Conectividad de red | Media | Medio | Redundancia de conexiones, modo offline básico |

### 7.2 Riesgos Operacionales

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Resistencia al cambio | Alta | Medio | Capacitación, comunicación de beneficios |
| Adopción lenta | Media | Medio | Incentivos, soporte en sitio |
| Errores de usuario | Media | Bajo | Interfaz intuitiva, validaciones |

### 7.3 Riesgos de Negocio

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| No alcanzar métricas objetivo | Media | Alto | Monitoreo continuo, ajustes iterativos |
| Costos de operación elevados | Baja | Medio | Análisis de costos, optimización |
| Problemas de privacidad | Baja | Alto | Cumplimiento normativo, auditorías |

---

## 8. Próximos Pasos

### 8.1 Documentación Complementaria

Los siguientes documentos están disponibles como complemento a estos requerimientos:

- **docs/ARQUITECTURA.md** - Diseño técnico y arquitectura del sistema
- **docs/PLAN-IMPLEMENTACION.md** - Plan detallado de implementación
- **docs/DEPLOY.md** - Guía de despliegue e instalación
- **docs/PRUEBAS.md** - Especificación de pruebas del sistema
- **docs/NFR-TESTS.md** - Pruebas de requerimientos no funcionales
- **docs/diagrams/** - Diagramas técnicos (contexto, secuencia, ER)

### 8.2 Validación Requerida

- [ ] Revisión y aprobación por stakeholders de negocio
- [ ] Validación técnica por equipo de arquitectura
- [ ] Revisión de seguridad y cumplimiento
- [ ] Estimación de esfuerzo y cronograma
- [ ] Definición de plan de pruebas

### 8.3 Consideraciones para Implementación

- Desarrollo iterativo con entregas incrementales
- Pruebas piloto en sucursal controlada
- Monitoreo continuo de métricas de adopción
- Retroalimentación constante de usuarios finales
- Plan de rollback en caso de problemas críticos

---

**Preparado por:** Área de Producto e Innovación  
**Tipo:** Proyecto de Capacitación - Ciclo Completo de Desarrollo de Software  
**Estado:** Listo para validación y diseño técnico

**Fuentes:**
- docs/REQUERIMIENTOS-NEGOCIO.md
- docs/REQUERIMIENTOS-FUNCIONALES.md