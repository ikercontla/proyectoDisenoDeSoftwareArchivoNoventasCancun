# Verificación comparativa · Entrega 2

Equipo 03 · Archivo Noventas Cancún

Se comparan los mismos 24 casos funcionales en `v1-antes` y `v2.1-refactorizado`, con las seis refactorizaciones aplicadas. Cada caso conserva sus entradas y resultados esperados. [Verificación inicial](verificacion-v1.md) · [Suite inicial](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v1-antes/src/test/java/mx/archivonoventas/AceptacionTest.java) · [Suite refactorizada](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v2.1-refactorizado/src/test/java/mx/archivonoventas/AceptacionTest.java)

El anexo reúne las entradas y resultados esperados. El registro de consola muestra los 24 casos de ambas ejecuciones. [Evidencia por caso](evidencia-casos.md) · [Salida de consola](salida-verificacion.txt)

| Caso | Comprobación | Antes · v1-antes | Después · v2.1-refactorizado |
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

Ejecutadas con Java 17, SQLite temporal y puerto local efímero. Los casos del mapa se ejercitaron con dobles del navegador y del SDK. Para el ajuste de RNF-03 también se usó Chromium con la solicitud de Google interceptada.

V-01a y V-01b también se repitieron con las mismas dos locaciones en las dos etiquetas. La prueba comparativa verifica ambos marcadores, sus fichas y el catálogo ante un fallo del SDK simulado. [Script de comparación](verificar-mapa-comparado.mjs) · [Salida de consola](salida-verificacion.txt)

| Revisión | Maven | Mapa simulado |
|---|---|---|
| Antes · v1-antes | 25 pruebas, 0 fallos, 0 errores | V-01a/b comprobados sobre el script original |
| Después · v2.1-refactorizado | 33 pruebas, 0 fallos, 0 errores | 7 de 7 |

La versión refactorizada produjo esta salida.

    Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS
    tests 7 / pass 7 / fail 0

Las pruebas adicionales cubren precisión de fechas, firmas de archivos, coherencia de formularios, tamaño en publicación directa, original ausente en la descarga en lote y reversión de la escritura de un original. Se reproducen en Windows desde la raíz del repositorio con estos comandos.

    .\mvnw.cmd verify
    node --test src/test/js/mapa.test.mjs

El segundo comando usa Node.js 24 y corresponde a la versión refactorizada. Las ejecuciones de cada refactorización figuran en la bitácora. [Bitácora](bitacora-refactorizacion.md) · [Pruebas del mapa](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v2.1-refactorizado/src/test/js/mapa.test.mjs)

## Requisitos no funcionales

| Requisito | Evidencia y estado |
|---|---|
| RNF-01 · Mapa con hasta 100 marcadores | Trabajo futuro. Medir el LCP con Google Maps real y una clave autorizada. Objetivo de hasta 2,5 s bajo las condiciones de red del SRS. |
| RNF-02 · Rendimiento del ZIP | La medición local de R-06 obtuvo P95 de **4,748 s**. El objetivo de P95 de hasta 4 s sigue pendiente. |
| RNF-03 · Contingencia de Google Maps | El catálogo está disponible desde la primera vista. Se verificó en Chromium que el aviso aparece en menos de 1 s al bloquear la solicitud o dejarla sin respuesta. El mapa aparece si después termina de cargar. |
| RNF-03 · Disponibilidad mensual | El 99,5 % se registra como supuesto operativo para esta entrega, sin verificación. Comprobarlo en operación requiere un entorno desplegado y sondas cada 60 s durante el periodo mensual. |

La prueba local de RNF-02 usó 50 documentos sintéticos de 2 MB, una solicitud de calentamiento y diez descargas simultáneas por HTTP en localhost. Midió desde el envío hasta recibir el ZIP completo. El P95 por rango más cercano fue **4,859 s** en R-05 y **4,748 s** en R-06. La [medición inicial](verificacion-v1.md) obtuvo **3,898 s** en una ejecución anterior. Se ensayó un búfer de compresión de 64 KiB en una copia de R-06 y obtuvo **4,755 s**. Se conservó el búfer original. [Requisitos no funcionales del SRS](../srs/seccion7_requerimientos_no_funcionales.md)

### Contingencia en el navegador

El ajuste funcional posterior para RNF-03 pasó 33 pruebas Java y 9 del mapa simulado.

| Escenario en Chromium | Resultado local |
|---|---|
| Solicitud a `maps.googleapis.com` bloqueada | Aviso de mapa no disponible a los **17 ms**. Las dos locaciones de prueba siguieron accesibles. |
| Solicitud retenida sin respuesta | Aviso de demora a los **801 ms**. El catálogo quedó visible y el panel de mapa siguió oculto. |
| Respuesta tardía del SDK en escritorio y de los mosaicos en móvil | Aviso de demora a los **800 ms** en ambos casos. El mapa apareció al completar la carga y el aviso se ocultó. |

Los tiempos se midieron desde que la vista solicitó el SDK hasta que cambió el aviso. La respuesta del proveedor se controló en el navegador para reproducir cada escenario. [Ajuste de contingencia](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/0aef0bdb9030fda11e93b9b52f7a1e13d8a00822)

### Trabajo futuro con Google Maps

La Entrega 2 usa el SDK simulado como evidencia de RF-01. En una fase posterior se configurará `GOOGLE_MAPS_API_KEY` con una clave autorizada y se repetirán V-01a y V-01b en el navegador con el proveedor real. RNF-01 se comprobará con Lighthouse, hasta 100 marcadores y las condiciones de red del SRS. El objetivo de LCP es de hasta 2,5 s. [Configuración del mapa](../../README.md#mapa) · [Requisitos no funcionales del SRS](../srs/seccion7_requerimientos_no_funcionales.md)
