# Archivo Noventas Cancún · Avance de la Entrega 2

Equipo 03 · Diseño de Software · 22 de noviembre de 2026

## Punto de partida

La revisión parte del commit [`168d357`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/168d357756eb0f3ed8c1f8f5ee4930ea2570f105) de la Entrega 1. Las ramas `main` y `revision-uml` contienen el SRS, las tarjetas CRC y el diagrama conceptual. Falta ubicar una versión ejecutable con RF-01 a RF-06 para registrar `v1-antes` y obtener evidencia del comportamiento inicial.

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

Queda pendiente ejecutar los 12 casos antes y después. Si el diagnóstico alcanza RF-07 a RF-12, incorporar los casos correspondientes del SRS y ejecutarlos sobre la versión inicial antes de aplicar el cambio.

[Criterios de aceptación del SRS](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion5_requerimientos_funcionales.md) · [Reglas de negocio](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion6_reglas_negocio.md) · [Requerimientos no funcionales](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion7_requerimientos_no_funcionales.md)

## Candidatos para el diagnóstico

Los puntos siguientes proceden del diseño conceptual. Para convertir cada uno en hallazgo, registrar la clase, el método o import y la línea exacta del código ejecutable. Estimar cuántas clases habría que tocar y contrastar la probabilidad de cambio con los supuestos de la Entrega 1 antes de elegir cuatro hallazgos.

| Qué inspeccionar | Riesgo a comprobar | Cambio que serviría para evaluar impacto |
|---|---|---|
| `Archivo`. La CRC reúne la colección, la cronología y la generación de ZIP. | Baja cohesión y SRP si el código mezcla la gestión de documentos con los detalles de compresión. | SUP-03. Aumento del número de documentos por locación. |
| `PropuestaAporte`. El UML describe una aprobación que crea un documento y lo incorpora al archivo. | Baja cohesión y SRP si la misma clase gestiona la transición de estado y la persistencia o publicación concreta. | SUP-04. Cambio de la política de conservación del aporte. |
| Pantalla de mapa y carga de la API externa. | Alto acoplamiento y DIP si la pantalla o la lógica de catálogo usan directamente tipos, eventos o inicialización de Google Maps. | DEP-01. Sustitución del proveedor por problemas recurrentes de cuota o disponibilidad. |
| Carga de aportes y descarga de originales (RF-03, RF-04). | Alto acoplamiento si varios componentes dependen de rutas locales o del mismo mecanismo concreto de almacenamiento. | DEP-02. Migración de volumen local a almacenamiento externo. |
| `Documento` y `DocumentoPDF`. El UML permite que preparen descargas y validen su contenido. | LSP si algún consumidor acepta un `Documento`, pero al recibir un `DocumentoPDF` presupone que hay imágenes o texto separado, o si la subclase rompe el contrato común. | Aplicar las mismas verificaciones de consulta y descarga a ambos tipos de publicación. |
| Validación de formato en aportes, imágenes y PDF. | OCP si agregar un formato obliga a editar varios condicionales dispersos. | SUP-02. Admisión futura de formatos con mayor volumen. |

[Tarjetas CRC](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/diseno/diseno_conceptual.md#42-tarjetas-crc-de-las-clases-principales) · [UML conceptual](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/diagramas/Equipo3_Entrega1_DiagramaClases.puml) · [Supuestos y dependencias](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/168d357756eb0f3ed8c1f8f5ee4930ea2570f105/docs/srs/seccion4_supuestos_dependencias.md)

## Métricas iniciales

Las tarjetas CRC de la Entrega 1 identifican `PropuestaAporte`, `Archivo` y `Documento` como las clases a medir. Aplicar ambos cálculos al código de `v1-antes`.

- **CBO**. Listar las clases que usa y las clases que la usan. Anotar para cada vínculo el atributo, firma, creación o llamada que lo demuestra. Unir ambos grupos y contar cada clase una sola vez. Excluir tipos de la biblioteca estándar, excepciones y enumeraciones.
- **LCOM**. Listar los métodos con los atributos propios que usa cada uno. Excluir constructores, getters y setters. Formar las `n(n−1)/2` parejas y marcar cada una como `P` si no comparte atributos o `Q` si comparte al menos uno. Verificar que `P + Q = n(n−1)/2` y calcular `máx(0, P − Q)`.

Los cálculos iniciales quedan pendientes hasta identificar las clases implementadas, sus referencias y los cuerpos de sus métodos. Repetir el cálculo con el mismo criterio sobre las mismas tres clases en la versión final.

## Secuencia de la entrega

1. Confirmar la revisión que ejecuta los seis Must. Conservarla con la etiqueta `v1-antes` y guardar la salida inicial de las verificaciones.
2. Documentar al menos seis hallazgos, incluidos dos de baja cohesión, dos de alto acoplamiento (uno con Google Maps) y tres principios SOLID distintos, con SRP y DIP. Priorizar al menos cuatro.
3. Registrar al menos cinco refactorizaciones en commits independientes con los IDs `R-xx` y `H-xx`. Cubrir tres categorías de técnicas, corregir dos violaciones SOLID y aislar el sistema externo detrás de una interfaz. Volver a ejecutar los casos afectados tras cada cambio.
4. Actualizar el PlantUML conforme al código, recalcular las métricas, contar las clases afectadas antes y después para un escenario de cambio y etiquetar la versión final `v2-refactorizado`.
5. Completar el PDF de entrega y el archivo fuente UML. Incluir en el PDF la bitácora, los fragmentos antes y después, las evidencias de verificación, la reflexión del equipo y el uso de IA.

## Uso de IA

Se usó OpenAI Codex para revisar las dos guías y la Entrega 1, y para preparar la matriz de casos y los puntos de inspección. La matriz recoge resultados esperados del SRS.
