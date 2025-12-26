Eres un Arquitecto de Software Senior y Technical Writer.

Tu tarea es GENERAR EL ARCHIVO README.md
del repositorio GitHub del Sistema Ticketero Digital.

El README debe:
- Ser el punto de entrada principal al repositorio
- Explicar qué es el sistema y cómo navegar su documentación
- Referenciar TODOS los artefactos relevantes del proyecto
- Organizar la información por propósito y rol
- NO duplicar el contenido de los documentos
- Usar enlaces relativos a archivos Markdown del repositorio

La salida debe ser:
- Markdown válido
- Lista para guardarse directamente como README.md
- SIN texto fuera del archivo

NO inventes información.
NO redefinas decisiones técnicas.
Usa exclusivamente los artefactos existentes.

---

## ARTEFACTOS DISPONIBLES EN EL REPOSITORIO

### Reglas técnicas (Amazon Q / estándares)
.amazonq/rules/
- 01-SPRING-BOOT-PATTERNS.md
- 02-JPA-ENTITIES-&-DATABASE.md
- 03-DTOs&VALIDATION.md
- 04-JAVA-21-FEATURES.md
- 05-LOMBOK.md

### Documentación funcional y técnica
docs/
- ARQUITECTURA.md
- BASE-DE-DATOS.md
- CODIGO.md
- DEPLOY.md
- ENDPOINTS.md
- PLAN-IMPLEMENTACION.md
- USO-SISTEMA.md
- PRUEBAS.md
- PROMPTS.md
- REQUERIMIENTOS.md
- REQUERIMIENTOS-FUNCIONALES.md
- REQUERIMIENTOS-NEGOCIO.md

### Pruebas No Funcionales (detalle)
docs/
- NFR-TESTS.md
- NFR-TEST-RESULTS.md
- FINAL-NFR-REPORT.md

### Prompts del proyecto
docs/prompts/
- Prompts de análisis, arquitectura, implementación, pruebas y deploy
- Prompts de generación de documentación (.md)

---

## ESTRUCTURA OBLIGATORIA DEL README.md

Genera el README con las siguientes secciones, en este orden:

### 1. Visión General
- Qué es el Sistema Ticketero Digital
- Problema que resuelve
- Tipo de sistema (empresarial / backend / event-driven)

### 2. Estructura del Repositorio
- Breve explicación de las carpetas principales
- docs/
- docs/prompts/
- .amazonq/rules/
- k6/ (si corresponde)

### 3. Requerimientos y Alcance
- Enlace a documentación de requerimientos
- Diferencia entre negocio y funcionales

### 4. Arquitectura del Sistema
- Resumen arquitectónico
- Enlace a ARQUITECTURA.md
- Referencia a diagramas existentes

### 5. Implementación y Código
- Stack tecnológico
- Enlace a CODIGO.md
- Enlace al plan de implementación

### 6. API y Modelo de Datos
- Enlace a ENDPOINTS.md
- Enlace a BASE-DE-DATOS.md
- Nota sobre versionamiento (Flyway)

### 7. Pruebas y Calidad
- Estrategia general de pruebas
- Enlace a PRUEBAS.md
- Enlace a documentos NFR (tests, resultados, reporte final)

### 8. Deployment e Infraestructura
- Enfoque de deploy
- Infraestructura como Código (AWS CDK)
- Enlace a DEPLOY.md
- Nota sobre dry-run vs deploy real

### 9. Prompts y Desarrollo Asistido por IA
- Rol de los prompts en el proyecto
- Diferencia entre:
  - Prompts que generan el sistema
  - Prompts que generan documentación
- Enlace a PROMPTS.md

### 10. Reglas y Convenciones Técnicas
- Propósito de las reglas Amazon Q
- Enlace a cada regla disponible
- Cómo se usan dentro del proyecto

### 11. Estado del Proyecto
- Estado actual del sistema (implementado, probado, deploy-ready)
- Qué está listo
- Qué queda fuera del alcance actual

### 12. Cómo Navegar el Proyecto (por rol)
- Negocio / Stakeholders
- Desarrollo
- QA
- DevOps / Infraestructura

---

## REGLAS DE REDACCIÓN

- Lenguaje claro, profesional y técnico
- No incluir código largo
- No repetir explicaciones completas
- Usar enlaces relativos (./docs/..., ./.amazonq/...)
- El README debe poder leerse en 5–10 minutos
