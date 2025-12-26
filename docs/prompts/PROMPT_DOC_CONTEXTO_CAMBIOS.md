Eres un Arquitecto de Software Senior y Technical Writer.

Tu tarea es realizar TRES acciones coordinadas en el repositorio
del Sistema Ticketero Digital.

NO inventes información.
NO redefinas decisiones técnicas.
NO cambies el contenido existente innecesariamente.
Usa exclusivamente los artefactos ya presentes en el repositorio.

---

## ACCIÓN 1: GENERAR CONTEXT-OF-CHANGES.md (RAÍZ DEL REPO)

Genera un archivo Markdown en la raíz del repositorio con el nombre:

CONTEXT-OF-CHANGES.md

Objetivo del documento:
- Explicar el enfoque incremental del repositorio
- Describir cómo el sistema y la documentación evolucionaron por hitos
- Explicar el uso de tags semánticos como mecanismo de trazabilidad
- Aclarar la relación entre código, documentación, pruebas e infraestructura
- Mencionar el uso de asistentes de IA (Amazon Q) como apoyo al proceso,
  desde una perspectiva técnica y de productividad (no académica)

Estructura sugerida:
- Introducción
- Enfoque de evolución del proyecto
- Convención de tags y su propósito
- Rol de los prompts en la evolución del sistema
- Cómo interpretar la historia del repositorio

El documento debe:
- Tener tono profesional y técnico
- Ser conciso (1–2 páginas máximo)
- Estar listo para versionarse y revisarse

---

## ACCIÓN 2: ACTUALIZAR README.md

Edita el archivo README.md existente para:

- Agregar una breve sección o nota (2–3 líneas) que indique que:
  - El repositorio fue construido de forma incremental
  - La evolución está documentada mediante tags y artefactos versionados
- Incluir un enlace relativo a CONTEXT-OF-CHANGES.md
- NO modificar ni reordenar otras secciones del README
- NO duplicar el contenido del nuevo documento

La modificación debe integrarse de forma natural
dentro del flujo del README.

---

## ACCIÓN 3: ACTUALIZAR docs/PROMPTS.md

Revisar todos los prompts existentes en el repositorio, en particular:
- docs/prompts/*
- Prompts utilizados para:
  - análisis
  - arquitectura
  - implementación
  - pruebas
  - deployment
  - generación de documentación (README, BD, código, etc.)

Actualizar el archivo docs/PROMPTS.md para:

- Incorporar los prompts que NO estén actualmente documentados
- Incluir específicamente los prompts de generación de documentación,
  como PROMPT_DOC_README.md y otros PROMPT_DOC_*.md
- Para cada prompt, documentar:
  - Nombre del prompt
  - Propósito
  - Artefacto(s) que genera o modifica
  - Rol dentro del ciclo de vida del proyecto
- Mantener coherencia de estilo con el contenido existente
- NO eliminar información previa, solo complementar y ordenar

Mejorar la usabilidad del documento docs/PROMPTS.md agregando un mapa visual de referencia rápida.

Acción: Insertar una sección "Mapa de Prompts y Artefactos" después de "Metodología de Desarrollo" que incluya:

Estructura de árbol ASCII organizando los prompts por fases

Iconos visuales para cada fase (📋 📚 🧪 ☁️ 🏗️)

Relación directa prompt → artefacto generado

Métricas resumidas del proyecto al final del mapa

---

## REGLAS DE SALIDA

- Todos los archivos deben ser Markdown válido
- Mostrar claramente el contenido completo del nuevo archivo
- Mostrar README.md con la modificación aplicada
- Mostrar docs/PROMPTS.md con las secciones nuevas agregadas
- NO incluir explicaciones fuera de los archivos generados
