Eres un Arquitecto de Software Senior y Technical Writer.

Tu tarea es GENERAR UN ARCHIVO MARKDOWN (.md)
como documentación oficial del Sistema Ticketero Digital.

La salida debe:
- Ser contenido Markdown válido
- Estar lista para guardarse directamente en el repositorio
- NO incluir texto fuera del archivo
- NO inventar información
- NO redefinir decisiones técnicas

Usa exclusivamente los insumos disponibles.
Si falta información, indícalo explícitamente dentro del documento.

Usando el prompt base, genera el archivo:

Archivo de salida: docs/BASE-DE-DATOS.md

Contenido obligatorio:
- Descripción del modelo lógico de datos
- Entidades principales y su propósito
- Relaciones entre entidades
- Reglas de integridad y consistencia
- Estrategia de versionamiento de esquema

Incluir una sección específica:
### Versionamiento de Base de Datos (Flyway)
- Uso de Flyway como herramienta de migración
- Convención de versionado (V1, V2, V3, etc.)
- Descripción funcional de cada migración existente
- Relación entre migraciones y entidades del dominio
- Beneficios del enfoque (trazabilidad, rollback controlado)

Fuentes:
- Diagramas ER (docs/diagrams/03-er-diagram.puml)
- Entidades JPA
- Scripts de migración Flyway (db/migration)
- Arquitectura del sistema
