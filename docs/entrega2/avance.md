# Archivo Noventas Cancún · Avance de la Entrega 2

Equipo 03 · Diseño de Software · 22 de noviembre de 2026

La etiqueta [`base-must`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/base-must) conserva la primera implementación Java con RF-01 a RF-06. La etiqueta [`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes) fija la revisión con RF-01 a RF-12 para la comparación de la Entrega 2. [Resultados de verificación](verificacion-v1.md)

## Punto de partida

La revisión parte del commit [`168d357`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/168d357756eb0f3ed8c1f8f5ee4930ea2570f105) de la Entrega 1. El SRS, las tarjetas CRC y el diagrama conceptual orientaron la implementación Java. La prueba con el proveedor real de mapas sigue pendiente y los dos casos del mapa ya se verificaron con simulación sobre el commit inicial.

## Casos de verificación preparados

Usar el mismo conjunto de datos antes y después. Como datos de prueba, preparar dos locaciones activas, una inactiva, un documento publicado, otro cuyo binario esté ausente y propuestas en estado `Revisada`. Restablecer los datos entre ejecuciones que modifiquen estados o contadores.

| ID | Entrada y acción | Resultado esperado | Evidencia por capturar |
|---|---|---|---|
| V-01a | Abrir el mapa con Google Maps disponible y seleccionar uno de los dos marcadores. | Se muestran las locaciones en sus coordenadas registradas y el marcador abre la ficha básica correspondiente. | Mapa, ficha y coordenadas de los marcadores. |
| V-01b | Bloquear las solicitudes a Google Maps en el navegador y recargar. | Se oculta el mapa y aparece el catálogo con todas las locaciones activas y sus coordenadas textuales. RNF-03 fija un plazo menor de 1 s para activar la alternativa ante el fallo. | Solicitud bloqueada, catálogo y tiempo medido desde el fallo. |
| V-02a | Abrir una locación activa con documentos vinculados. | La ficha devuelve la descripción histórica y su galería de documentos. | Ficha con la descripción y los documentos asociados. |
| V-02b | Abrir por identificador una locación inexistente o inactiva. Repetir con el otro estado. | La solicitud registra un 404, la aplicación informa que la locación no existe y devuelve al mapa general. | Estado HTTP, mensaje y vista final. |
| V-03a | Descargar un PDF publicado con binario disponible. | El navegador descarga el mismo binario con su nombre original. | Nombre del archivo descargado y comparación de su hash con el original. |
| V-03b | Descargar un documento publicado cuyo binario está ausente. | La aplicación registra la anomalía y muestra «El documento solicitado no está disponible temporalmente para descarga». | Mensaje y entrada de registro. |
| V-04a | Enviar sin sesión un PDF válido de 1 MB, una locación activa y una descripción. | Se guarda una propuesta `Pendiente` y se muestra una confirmación con folio único. | Folio en la respuesta y registro de la propuesta. |
| V-04b | Enviar la misma solicitud sin adjunto y repetir con un ZIP. | Se bloquean ambos envíos, se conserva el total de propuestas y se muestra «Debe adjuntar un archivo válido (.jpg, .png, .webp, .pdf de hasta 25 MB)». | Mensaje y recuento de propuestas antes y después de cada envío. |
| V-05a | Iniciar sesión con una cuenta de administración de prueba y contraseña válida. | Se inicia la sesión y se abre la consola de administración. | Redirección y acceso al panel, con las credenciales ocultas. |
| V-05b | Probar la misma cuenta con contraseña incorrecta. | El acceso se deniega, los intentos fallidos aumentan en uno y se muestra «Credenciales no válidas. Verifique sus datos». | Mensaje, contador y denegación de acceso al panel. |
| V-06a | Aprobar como administrador una propuesta `Revisada`. | Pasa a `Aprobada`, se crea un documento público y aparece en el archivo de la locación correspondiente. | Estado antes y después, ID del documento y galería pública. |
| V-06b | Intentar rechazar otra propuesta `Revisada` con el motivo vacío. | Permanece `Revisada` y se muestra «Debe ingresar obligatoriamente una justificación para rechazar la propuesta». | Mensaje y estado de la propuesta. |

Los resultados de la primera ejecución están en el [registro de verificación](verificacion-v1.md). Si el diagnóstico alcanza RF-07 a RF-12, incorporar los casos correspondientes del SRS y ejecutarlos sobre la versión inicial antes de aplicar el cambio.

[Criterios de aceptación del SRS](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion5_requerimientos_funcionales.md) · [Reglas de negocio](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion6_reglas_negocio.md) · [Requerimientos no funcionales](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion7_requerimientos_no_funcionales.md)

## Diagnóstico y métricas iniciales

Se documentaron seis hallazgos y se priorizaron cinco. Los cálculos CBO y LCOM corresponden a `PropuestaAporte`, `Archivo` y `Documento`, las tres clases de las tarjetas CRC. [Hallazgos, evidencia y cálculos](diagnostico-v1.md)

## Secuencia de la entrega

1. Etiquetar la revisión inicial y guardar la evidencia. Completado para el código y las pruebas locales de `v1-antes`. La prueba con Google Maps real está registrada como pendiente.
2. Documentar hallazgos, priorizar y medir CBO y LCOM. Completado en el [diagnóstico](diagnostico-v1.md).
3. Aplicar las refactorizaciones en commits independientes y volver a ejecutar los casos afectados tras cada cambio. El [plan de fase 2](plan-fase-2.md) identifica cinco cambios, sus técnicas y sus casos de regresión.
4. Actualizar el PlantUML conforme al código, recalcular las métricas, contar las clases afectadas antes y después para un escenario de cambio y etiquetar la versión final `v2-refactorizado`.
5. Completar el PDF de entrega y el archivo fuente UML. Incluir en el PDF la bitácora, los fragmentos antes y después, las evidencias de verificación, la reflexión del equipo y el uso de IA.

## Uso de IA

Se usó OpenAI Codex para revisar las dos guías y la Entrega 1, y para preparar la matriz de casos y los puntos de inspección. La matriz recoge resultados esperados del SRS.
