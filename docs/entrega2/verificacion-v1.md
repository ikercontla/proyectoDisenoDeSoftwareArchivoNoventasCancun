# Verificación de la implementación

La etiqueta [`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes) apunta al commit `5d152e4` con RF-01 a RF-12. La etiqueta [`base-must`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/base-must) conserva el commit `4366f4b` con RF-01 a RF-06. Las pruebas se ejecutaron con Java 17, una base SQLite temporal y un servidor HTTP de puerto efímero.

## Resultados de `mvnw verify`

| Caso | Resultado inicial | Evidencia de la aserción |
|---|---|---|
| V-01a | Datos y simulación verificados | Java entrega coordenadas y enlaces. Con Google Maps simulado se confirmó la posición del marcador y la ficha al seleccionarlo. |
| V-01b | Catálogo y simulación verificados | La página muestra las locaciones activas. Al disparar el error del script se ocultó el mapa y se mostró el aviso del catálogo. |
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
| V-07a | Cumple | La línea del tiempo ordena las fechas y excluye documentos sin fecha. |
| V-07b | Cumple | Se oculta con cero documentos, un solo documento o dos fechas que corresponden al mismo día de orden. |
| V-08a | Cumple | El ZIP contiene los originales con sus bytes intactos y distingue nombres repetidos. |
| V-08b | Cumple | Permite exactamente 100 MB, rechaza el exceso y exige al menos un documento. |
| V-09a | Cumple | El alta autenticada devuelve la nueva ficha y las coordenadas aparecen en los datos del mapa. |
| V-09b | Cumple | Los nombres duplicados y las coordenadas inválidas se señalan por campo. La operación no crea una locación. |
| V-10a | Cumple | La publicación directa queda visible y el original se puede descargar. |
| V-10b | Cumple | Faltar la locación o la fecha muestra los campos obligatorios sin guardar binarios ni documentos. |
| V-11a | Cumple | Los enlaces en la descripción navegan a una locación o documento existente. |
| V-11b | Cumple | Un destino inactivo devuelve al documento de origen y muestra el aviso. |
| V-12a | Cumple | Se filtran las locaciones por nombre o reseña y se entregan al mapa únicamente las coincidencias. |
| V-12b | Cumple | Una búsqueda sin coincidencias mantiene el filtro previo y muestra el aviso. |

Pasaron 25 pruebas, incluida la compatibilidad de inicio de sesión con un hash generado por la versión Node.js. Las fechas parciales se ordenan por el primer día de su periodo, de acuerdo con SUP-01.

El 22 de noviembre de 2026 se repitió la suite desde una copia limpia del commit inicial. El resumen de Maven fue el siguiente.

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Los dos casos simulados del mapa se ejecutaron además sobre el script de esa misma revisión. [Suite de aceptación](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/5d152e48257858de55c34d4d4ce5e8b36da68ecb/src/test/java/mx/archivonoventas/AceptacionTest.java)

## Medición de rendimiento

Para RNF-02 se midieron diez descargas HTTP concurrentes en localhost después de una solicitud de calentamiento. El archivo de prueba tenía 50 documentos sintéticos de 2 MB cada uno. Se contó desde el envío de cada solicitud hasta recibir el ZIP completo. El P95 local por rango más cercano fue de **3,898 s** frente al máximo de 4 s del SRS.

Queda pendiente ejecutar V-01a y V-01b con una clave autorizada de Google Maps, medir RNF-01 y comprobar el tiempo de contingencia de RNF-03. La disponibilidad mensual de RNF-03 requiere un entorno desplegado y monitoreado. La comparación posterior se registrará durante la [fase 2](plan-fase-2.md).

## Uso de IA

Se usó OpenAI Codex para implementar la base, migrar el servidor a Java, añadir RF-07 a RF-12 y ejecutar las pruebas y la medición local.
