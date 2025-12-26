# Sistema Ticketero Digital

## Visión General

El **Sistema Ticketero Digital** es una solución empresarial backend para la gestión automatizada de tickets de soporte técnico. Implementa un sistema event-driven que procesa solicitudes de usuarios, las asigna automáticamente a asesores disponibles y gestiona el ciclo completo de atención.

**Problema que resuelve:**
- Asignación manual ineficiente de tickets de soporte
- Falta de trazabilidad en el proceso de atención
- Sobrecarga de asesores sin distribución equitativa
- Ausencia de métricas de performance y SLA

**Tipo de sistema:** Backend empresarial con arquitectura event-driven, API REST y procesamiento asíncrono de eventos.

## Estructura del Repositorio

```
ticketero-digital/
├── docs/                    # Documentación técnica y funcional
├── docs/prompts/           # Prompts de desarrollo asistido por IA
├── .amazonq/rules/         # Reglas y convenciones técnicas
├── k6/                     # Scripts de pruebas de performance (K6)
├── scripts/                # Scripts de pruebas no funcionales
├── src/                    # Código fuente de la aplicación
└── infrastructure/         # Infraestructura como código (AWS CDK)
```

### Carpetas Principales

- **`docs/`**: Documentación completa del proyecto (arquitectura, API, base de datos, deployment)
- **`docs/prompts/`**: Prompts utilizados para generar el sistema y su documentación
- **`.amazonq/rules/`**: Estándares de desarrollo y buenas prácticas técnicas
- **`k6/`**: Pruebas de performance y carga con K6
- **`scripts/`**: Scripts de pruebas no funcionales (NFR)

## Requerimientos y Alcance

El sistema está diseñado para cumplir requerimientos específicos de negocio y funcionales:

- **[Requerimientos de Negocio](./docs/REQUERIMIENTOS-NEGOCIO.md)**: Objetivos empresariales y métricas de éxito
- **[Requerimientos Funcionales](./docs/REQUERIMIENTOS-FUNCIONALES.md)**: Casos de uso y funcionalidades específicas
- **[Requerimientos Generales](./docs/REQUERIMIENTOS.md)**: Consolidado de todos los requerimientos del sistema

La diferencia clave es que los requerimientos de negocio definen el **qué** y **por qué**, mientras que los funcionales especifican el **cómo** técnico.

## Arquitectura del Sistema

El sistema implementa una **arquitectura hexagonal** con separación clara de responsabilidades:

- **Capa de Aplicación**: Controllers REST y manejo de eventos
- **Capa de Dominio**: Lógica de negocio y entidades
- **Capa de Infraestructura**: Persistencia, integraciones externas y schedulers

**Stack Tecnológico:**
- Java 21 + Spring Boot 3.x
- PostgreSQL + Flyway
- Docker + Docker Compose
- AWS CDK para infraestructura

📖 **Documentación detallada:** [ARQUITECTURA.md](./docs/ARQUITECTURA.md)

## Implementación y Código

El desarrollo sigue patrones establecidos de Spring Boot con énfasis en:

- Inyección de dependencias por constructor
- Separación estricta Controller → Service → Repository
- DTOs con Records (Java 21)
- Manejo centralizado de excepciones

**Recursos técnicos:**
- **[Guía de Código](./docs/CODIGO.md)**: Estructura, patrones y convenciones
- **[Plan de Implementación](./docs/PLAN-IMPLEMENTACION.md)**: Roadmap técnico y fases de desarrollo

## API y Modelo de Datos

### API REST
La API expone endpoints para gestión completa del ciclo de tickets:
- Creación y consulta de tickets
- Gestión de asesores y disponibilidad
- Métricas y reportes del sistema

📖 **Documentación completa:** [ENDPOINTS.md](./docs/ENDPOINTS.md)

### Base de Datos
Modelo relacional optimizado con:
- Entidades principales: Tickets, Asesores, Clientes
- Versionamiento con Flyway migrations
- Índices optimizados para consultas frecuentes

📖 **Esquema y diseño:** [BASE-DE-DATOS.md](./docs/BASE-DE-DATOS.md)

## Pruebas y Calidad

### Estrategia de Pruebas
El sistema implementa una estrategia integral de testing:

- **Unitarias**: Cobertura de lógica de negocio
- **Integración**: Validación de componentes
- **No Funcionales**: Performance, concurrencia y resiliencia

### Pruebas No Funcionales (NFR)
Validación exhaustiva de requisitos de performance:

- **[Especificación NFR](./docs/NFR-TESTS.md)**: Tests implementados y criterios
- **[Resultados de Pruebas](./docs/NFR-TEST-RESULTS.md)**: Métricas y análisis detallado
- **[Reporte Final NFR](./docs/FINAL-NFR-REPORT.md)**: Conclusiones y recomendaciones

📖 **Guía completa:** [PRUEBAS.md](./docs/PRUEBAS.md)

## Deployment e Infraestructura

### Enfoque de Deployment
- **Infraestructura como Código**: AWS CDK con TypeScript
- **Containerización**: Docker multi-stage builds
- **Orquestación**: ECS Fargate con Application Load Balancer

### Modalidades
- **Dry-run**: Validación de infraestructura sin deploy real
- **Deploy completo**: Despliegue en AWS con todos los componentes

📖 **Guía de deployment:** [DEPLOY.md](./docs/DEPLOY.md)

**Nota importante:** El proyecto incluye toda la infraestructura necesaria pero se ejecuta en modo dry-run por defecto para evitar costos de AWS.

## Prompts y Desarrollo Asistido por IA

El proyecto utiliza desarrollo asistido por IA con dos tipos de prompts:

### Prompts de Sistema
Generan la implementación técnica:
- Análisis de requerimientos
- Diseño de arquitectura
- Implementación de código
- Configuración de infraestructura

### Prompts de Documentación
Generan la documentación del proyecto:
- Documentos técnicos (.md)
- Diagramas de arquitectura
- Guías de uso y deployment

📖 **Catálogo completo:** [PROMPTS.md](./docs/PROMPTS.md)

Todos los prompts están disponibles en [`docs/prompts/`](./docs/prompts/) organizados por categoría y propósito.

## Reglas y Convenciones Técnicas

El proyecto sigue estándares estrictos definidos en las reglas de Amazon Q:

- **[Spring Boot Patterns](./.amazonq/rules/01-SPRING-BOOT-PATTERNS.md)**: Arquitectura en capas, inyección de dependencias, patrones Controller/Service/Repository
- **[JPA Entities & Database](./.amazonq/rules/02-JPA-ENTITIES-&-DATABASE.md)**: Entidades JPA, relaciones, queries y migrations Flyway
- **[DTOs & Validation](./.amazonq/rules/03-DTOs&VALIDATION.md)**: Records, validaciones Jakarta, manejo de errores
- **[Java 21 Features](./.amazonq/rules/04-JAVA-21-FEATURES.md)**: Text blocks, pattern matching, virtual threads, sealed classes
- **[Lombok Best Practices](./.amazonq/rules/05-LOMBOK.md)**: Uso correcto de anotaciones Lombok

Estas reglas se aplican automáticamente durante el desarrollo y garantizan consistencia en todo el código.

## Estado del Proyecto

### ✅ Completado
- **Análisis y Diseño**: Requerimientos, arquitectura y modelo de datos
- **Implementación**: Sistema completo funcional con todas las características
- **Pruebas**: Suite completa de pruebas unitarias, integración y NFR
- **Documentación**: Documentación técnica exhaustiva
- **Infraestructura**: CDK templates listos para deployment

### ✅ Validado
- **Performance**: Throughput ≥50 tickets/min, latencia p95 <2s
- **Concurrencia**: 0 race conditions detectadas
- **Resiliencia**: Recovery time <90s tras fallos
- **Consistencia**: 0 inconsistencias en datos

### 🔄 Fuera del Alcance Actual
- Integración real con Telegram (implementado como mock)
- Deployment en AWS (modo dry-run por defecto)
- Monitoreo avanzado (APM, alertas)
- Autenticación y autorización de usuarios

## Cómo Navegar el Proyecto

### 👔 Para Stakeholders de Negocio
1. **[Requerimientos de Negocio](./docs/REQUERIMIENTOS-NEGOCIO.md)**: Objetivos y métricas
2. **[Uso del Sistema](./docs/USO-SISTEMA.md)**: Cómo funciona desde perspectiva de usuario
3. **[Reporte Final NFR](./docs/FINAL-NFR-REPORT.md)**: Validación de performance

### 👨‍💻 Para Desarrolladores
1. **[Código](./docs/CODIGO.md)**: Estructura y patrones de implementación
2. **[Arquitectura](./docs/ARQUITECTURA.md)**: Diseño técnico detallado
3. **[Reglas Técnicas](./.amazonq/rules/)**: Estándares de desarrollo
4. **[Plan de Implementación](./docs/PLAN-IMPLEMENTACION.md)**: Roadmap técnico

### 🧪 Para QA
1. **[Pruebas](./docs/PRUEBAS.md)**: Estrategia y tipos de testing
2. **[NFR Tests](./docs/NFR-TESTS.md)**: Especificación de pruebas no funcionales
3. **[Resultados NFR](./docs/NFR-TEST-RESULTS.md)**: Métricas y análisis

### 🚀 Para DevOps/Infraestructura
1. **[Deploy](./docs/DEPLOY.md)**: Guía de despliegue y configuración
2. **[Base de Datos](./docs/BASE-DE-DATOS.md)**: Esquema y migrations
3. **[Endpoints](./docs/ENDPOINTS.md)**: API para monitoreo y health checks

---

**Versión del Sistema:** 1.0  
**Stack:** Java 21 + Spring Boot 3.x + PostgreSQL + AWS  
**Estado:** ✅ Listo para deployment