# TICKETERO - Pruebas No Funcionales (NFR)

## Resumen

Este documento describe las pruebas no funcionales implementadas para validar los requisitos de **performance**, **concurrencia** y **resiliencia** del sistema Ticketero.

## Requisitos No Funcionales Validados

| ID | Requisito | Métrica | Umbral | Test |
|----|-----------|---------|--------|------|
| RNF-01 | Throughput | Tickets procesados/minuto | ≥ 50 | Load Test |
| RNF-02 | Latencia API | p95 response time | < 2 segundos | Load Test |
| RNF-03 | Concurrencia | Race conditions | 0 detectadas | Race Condition Test |
| RNF-04 | Consistencia | Tickets inconsistentes | 0 | Consistency Validator |
| RNF-05 | Recovery Time | Detección falla | < 90 segundos | Crash Tests |
| RNF-06 | Disponibilidad | Uptime durante carga | 99.9% | All Tests |
| RNF-07 | Recursos | Memory leak | 0 (estable 30 min) | Soak Test |

## Estructura de Tests

```
scripts/
├── performance/
│   ├── load-test.sh          # PERF-01: Carga sostenida
│   ├── spike-test.sh         # PERF-02: Picos de carga
│   └── soak-test.sh          # PERF-03: Estabilidad prolongada
├── concurrency/
│   ├── race-condition-test.sh # CONC-01: Race conditions
│   └── idempotency-test.sh   # CONC-02: Idempotencia
├── resilience/
│   ├── app-crash-test.sh     # RES-01: Crash aplicación
│   └── db-failure-test.sh    # RES-02: Falla base datos
├── utils/
│   ├── metrics-collector.sh  # Recolector métricas
│   └── validate-consistency.sh # Validador consistencia
└── run-nfr-tests.sh         # Ejecutor completo
```

## Tests Implementados

### 1. Performance Tests

#### PERF-01: Load Test Sostenido
- **Objetivo:** Validar throughput ≥50 tickets/min y latencia p95 <2s
- **Escenario:** 100 tickets en 2 minutos con 10 usuarios concurrentes
- **Métricas:** Throughput, latencia, error rate, completion rate
- **Criterios:** 
  - Throughput ≥ 50 tickets/min
  - Completion rate ≥ 99%
  - Sistema consistente post-test

#### PERF-02: Spike Test
- **Objetivo:** Validar comportamiento bajo carga súbita
- **Escenario:** 50 tickets simultáneos en <10 segundos
- **Métricas:** Tiempo de procesamiento, errores
- **Criterios:** Todos procesados en <180s

#### PERF-03: Soak Test
- **Objetivo:** Detectar memory leaks y degradación
- **Escenario:** 30 tickets/min durante 30 minutos (configurable)
- **Métricas:** Memoria inicial vs final
- **Criterios:** Incremento memoria <20%

### 2. Concurrency Tests

#### CONC-01: Race Condition Test
- **Objetivo:** Validar que no hay race conditions en asignación de asesores
- **Escenario:** 1 asesor disponible, 5 tickets simultáneos
- **Métricas:** Asignaciones dobles, deadlocks
- **Criterios:** 
  - 0 asignaciones dobles
  - 0 deadlocks PostgreSQL
  - Procesamiento serializado

#### CONC-02: Idempotency Test
- **Objetivo:** Validar que tickets duplicados no se reprocesan
- **Escenario:** Crear ticket, completar, intentar duplicado
- **Métricas:** Contadores de tickets, mensajes, asesores
- **Criterios:** Duplicados rechazados o manejados correctamente

### 3. Resilience Tests

#### RES-01: Application Crash Test
- **Objetivo:** Validar recovery tras crash de aplicación
- **Escenario:** Crear tickets, crash app, restart, validar
- **Métricas:** Tiempo restart, tickets preservados
- **Criterios:**
  - Restart <60s
  - 0 tickets perdidos
  - Asesores liberados

#### RES-02: Database Failure Test
- **Objetivo:** Validar manejo de fallas de BD
- **Escenario:** Detener PostgreSQL, intentar operaciones, restart
- **Métricas:** Errores durante caída, tiempo recovery
- **Criterios:**
  - Errores esperados durante caída
  - Recovery exitoso <30s

## Herramientas de Soporte

### Metrics Collector
- Recolecta métricas cada 5 segundos
- CPU/memoria de contenedores
- Conexiones DB, tickets por estado
- Output: CSV para análisis

### Consistency Validator
- Valida estado del sistema post-test
- Detecta tickets inconsistentes
- Verifica asesores en estado inválido
- Cuenta duplicados potenciales

## Ejecución

### Ejecución Individual
```bash
# Performance
./scripts/performance/load-test.sh
./scripts/performance/spike-test.sh
./scripts/performance/soak-test.sh 30

# Concurrency
./scripts/concurrency/race-condition-test.sh
./scripts/concurrency/idempotency-test.sh

# Resilience
./scripts/resilience/app-crash-test.sh
./scripts/resilience/db-failure-test.sh
```

### Ejecución Completa
```bash
# Suite completa
./scripts/run-nfr-tests.sh

# Modo rápido (sin soak test largo)
./scripts/run-nfr-tests.sh --quick
```

## Interpretación de Resultados

### Códigos de Salida
- `0`: Test PASSED
- `1`: Test FAILED

### Archivos Generados
- `results/nfr-YYYYMMDD-HHMMSS/`: Directorio de resultados
- `*.log`: Logs detallados por test
- `*-metrics-*.csv`: Métricas del sistema
- `NFR-TEST-SUMMARY.md`: Reporte consolidado

### Métricas Clave
- **Throughput**: tickets/minuto procesados
- **Latencia p95**: percentil 95 de tiempo respuesta
- **Error Rate**: % de requests fallidos
- **Memory Growth**: incremento memoria durante soak test
- **Recovery Time**: tiempo para recuperarse de fallas

## Umbrales de Aceptación

| Métrica | Umbral | Justificación |
|---------|--------|---------------|
| Throughput | ≥50 tickets/min | Capacidad mínima requerida |
| Latencia p95 | <2000ms | Experiencia usuario aceptable |
| Error Rate | <1% | Alta confiabilidad |
| Memory Growth | <20% | Sin memory leaks significativos |
| Recovery Time | <90s | Disponibilidad alta |
| Consistency | 0 errores | Integridad datos crítica |

## Limitaciones Actuales

1. **Arquitectura Simplificada**: Tests adaptados a scheduler-based en lugar de RabbitMQ workers
2. **Telegram Mock**: No se valida integración real con Telegram
3. **Carga Limitada**: Tests diseñados para entorno desarrollo
4. **Métricas Básicas**: Sin APM avanzado (New Relic, DataDog)

## Mejoras Futuras

1. **Chaos Engineering**: Inyección fallas de red, CPU, disco
2. **Load Testing Avanzado**: K6 con escenarios complejos
3. **Monitoring**: Integración con Prometheus/Grafana
4. **CI/CD Integration**: Ejecución automática en pipeline
5. **Performance Regression**: Comparación histórica resultados

## Contacto

Para dudas sobre las pruebas NFR, contactar al equipo de Performance Engineering.