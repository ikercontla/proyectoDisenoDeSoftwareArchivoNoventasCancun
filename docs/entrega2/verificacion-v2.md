# Verificación comparativa · Entrega 2

Equipo 03 · Archivo Noventas Cancún

Los 24 casos funcionales conservaron sus entradas, sus resultados esperados y su método de verificación. Cada caso se cotejó con los informes de Maven de ambas revisiones. [Verificación inicial](verificacion-v1.md) · [Suite inicial](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v1-antes/src/test/java/mx/archivonoventas/AceptacionTest.java) · [Suite refactorizada](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/test/java/mx/archivonoventas/AceptacionTest.java)

| Caso | Comprobación | v1-antes | v2-refactorizado |
|---|---|---|---|
| V-01a | Java entrega coordenadas y enlaces. Con Google Maps simulado se confirmó la posición del marcador y la ficha al seleccionarlo. | Simulación y recursos | Simulación y recursos |
| V-01b | La página muestra las locaciones activas. Al disparar el error del script se ocultó el mapa y se mostró el aviso del catálogo. | Simulación y recursos | Simulación y recursos |
| V-02a | HTTP 200 con reseña, documento y enlace de descarga. | Cumple | Cumple |
| V-02b | HTTP 404 para locación inexistente e inactiva, con aviso y retorno al mapa. | Cumple | Cumple |
| V-03a | HTTP 200 con bytes idénticos al PDF y nombre original con acento en la cabecera. | Cumple | Cumple |
| V-03b | HTTP 404 con el mensaje del SRS y una entrada en el registro de la aplicación. | Cumple | Cumple |
| V-04a | El envío anónimo crea una propuesta `Pendiente` y devuelve el folio. | Cumple | Cumple |
| V-04b | Se rechazan adjuntos ausentes, inválidos o mayores de 25 MB sin crear propuestas. | Cumple | Cumple |
| V-05a | Credenciales válidas generan una cookie de sesión y permiten abrir el panel. | Cumple | Cumple |
| V-05b | HTTP 401, incremento del contador de intentos y acceso al panel denegado. | Cumple | Cumple |
| V-06a | La propuesta revisada pasa a `Aprobada` y publica un solo documento en la locación. | Cumple | Cumple |
| V-06b | Sin motivo se conserva `Revisada`. Con motivo se registra `Rechazada`. | Cumple | Cumple |
| V-07a | La línea del tiempo ordena las fechas y excluye documentos sin fecha. | Cumple | Cumple |
| V-07b | Se oculta con cero documentos, un solo documento o dos fechas que corresponden al mismo día de orden. | Cumple | Cumple |
| V-08a | El ZIP contiene los originales con sus bytes intactos y distingue nombres repetidos. | Cumple | Cumple |
| V-08b | Permite exactamente 100 MB, rechaza el exceso y exige al menos un documento. | Cumple | Cumple |
| V-09a | El alta autenticada devuelve la nueva ficha y las coordenadas aparecen en los datos del mapa. | Cumple | Cumple |
| V-09b | Los nombres duplicados y las coordenadas inválidas se señalan por campo. La operación no crea una locación. | Cumple | Cumple |
| V-10a | La publicación directa queda visible y el original se puede descargar. | Cumple | Cumple |
| V-10b | Faltar la locación o la fecha muestra los campos obligatorios sin guardar binarios ni documentos. | Cumple | Cumple |
| V-11a | Los enlaces en la descripción navegan a una locación o documento existente. | Cumple | Cumple |
| V-11b | Un destino inactivo devuelve al documento de origen y muestra el aviso. | Cumple | Cumple |
| V-12a | Se filtran las locaciones por nombre o reseña y se entregan al mapa únicamente las coincidencias. | Cumple | Cumple |
| V-12b | Una búsqueda sin coincidencias mantiene el filtro previo y muestra el aviso. | Cumple | Cumple |

## Salida de pruebas

Ejecutadas con Java 17, SQLite temporal y puerto local efímero. Los casos del mapa se ejercitaron con dobles del navegador y del SDK. La validación con el proveedor real y la disponibilidad mensual en un entorno desplegado siguen pendientes.

| Revisión | Maven | Mapa simulado |
|---|---|---|
| v1-antes | 25 pruebas, 0 fallos, 0 errores | V-01a/b comprobados sobre el script original |
| R-01 | 25 pruebas, 0 fallos, 0 errores | 7 de 7 |
| R-02 | 25 pruebas, 0 fallos, 0 errores | 7 de 7 |
| R-03 | 27 pruebas, 0 fallos, 0 errores | 7 de 7 |
| R-04 | 30 pruebas, 0 fallos, 0 errores | 7 de 7 |
| R-05 | 31 pruebas, 0 fallos, 0 errores | 7 de 7 |

En la verificación final se obtuvo:

    Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS
    tests 7 / pass 7 / fail 0

Las pruebas adicionales cubren precisión de fechas, firmas de archivos, coherencia de formularios y el error de tamaño en publicación directa. Para reproducirlas en Windows desde la raíz del repositorio, ejecutar:

    .\mvnw.cmd verify
    node --test src/test/js/mapa.test.mjs

El segundo comando usa Node.js 24 y corresponde a la versión refactorizada. [Bitácora](bitacora-refactorizacion.md) · [Pruebas del mapa](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/test/js/mapa.test.mjs)
