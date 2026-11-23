# Primera implementación · Verificación inicial

La implementación en Java 17 con Spring Boot mantiene los seis RF Must y el esquema SQLite de la versión anterior. Las locaciones y el PDF de demostración son datos de prueba.

## Resultados de `mvnw verify`

| Caso | Resultado inicial | Evidencia de la aserción |
|---|---|---|
| V-01a | Datos y recursos verificados | El servidor entrega las coordenadas, el enlace a la ficha y el script de marcadores que ya se probó con la API simulada. |
| V-01b | Catálogo alternativo verificado | La página muestra las locaciones activas con sus coordenadas y sirve el script que oculta el mapa si falla la carga. |
| V-02a | Cumple | HTTP 200 con reseña, documento y enlace de descarga. |
| V-02b | Cumple | HTTP 404 para locación inexistente e inactiva, con aviso y retorno al mapa. |
| V-03a | Cumple | HTTP 200 con bytes idénticos al PDF y nombre original con acento en la cabecera. |
| V-03b | Cumple | HTTP 404 con el mensaje del SRS y una entrada en el registro de la aplicación. |
| V-04a | Cumple | El envío anónimo crea una propuesta `Pendiente` y devuelve el folio. |
| V-04b | Cumple | Se rechazan adjuntos ausentes, inválidos o mayores de 25 MB sin crear propuestas. |
| V-05a | Cumple | Credenciales válidas generan una cookie de sesión y permiten abrir el panel. |
| V-05b | Cumple | HTTP 401, incremento del contador de intentos y acceso al panel denegado. |
| V-06a | Cumple | La propuesta revisada pasa a `Aprobada` y publica un solo documento en la locación. |
| V-06b | Cumple | Sin motivo se conserva `Revisada`. Con motivo se registra `Rechazada`. |

Pasaron 13 pruebas, incluida la compatibilidad de inicio de sesión con un hash generado por la versión Node.js. El JAR de Java 17 arrancó con los datos de demostración y se revisaron en el navegador el catálogo alternativo, la ficha del documento y el formulario de aportes con la locación preseleccionada.

Queda pendiente ejecutar V-01a y V-01b con una clave autorizada de Google Maps y registrar el tiempo de contingencia de RNF-03. La etiqueta `v1-antes` queda pendiente de esa validación. La comparación posterior se completará tras la refactorización.

## Uso de IA

Se usó OpenAI Codex para implementar la base a partir del SRS, migrar el servidor a Java y las vistas a plantillas HTML, y ejecutar la suite de aceptación.
