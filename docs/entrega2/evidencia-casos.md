# Evidencia por caso · Entrega 2

Equipo 03 · Archivo Noventas Cancún

La tabla compara dos versiones. **Antes** corresponde a `v1-antes` y **Después** a `v2.1-refactorizado`, que incluye R-01 a R-06. Se ejecutaron los mismos 24 casos de aceptación en ambas versiones con Java 17, SQLite temporal y un servidor local. [Salida de consola por caso](salida-verificacion.txt)

Los 24 casos pasaron en ambas versiones. Cada resultado se cotejó con el informe JUnit de Maven Surefire. Los segundos corresponden a la duración del método de prueba en Java. V-01a y V-01b también se verificaron con Google Maps simulado antes y después. [Código de las pruebas antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v1-antes/src/test/java/mx/archivonoventas/AceptacionTest.java) · [Código de las pruebas después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v2.1-refactorizado/src/test/java/mx/archivonoventas/AceptacionTest.java)

## RF-01 a RF-06

| Caso | RF | Tipo | Entrada y acción | Resultado esperado | Antes | Después |
|---|---|---|---|---|---|---|
| V-01a | RF-01 | Correcto | Abrir el mapa con dos locaciones activas y una inactiva. Seleccionar un marcador simulado. | La página entrega coordenadas y ruta. El marcador está en su posición y abre la ficha con el enlace. | Cumple · 0,029 s | Cumple · 0,032 s |
| V-01b | RF-01 | Error | Activar el error de carga del SDK simulado. | Se oculta el panel del mapa y el catálogo muestra las locaciones activas con sus coordenadas. | Cumple · 1,833 s | Cumple · 1,781 s |
| V-02a | RF-02 | Correcto | Abrir la ficha de la locación 1, que tiene un documento publicado. | HTTP 200 con reseña, documento, descarga y enlace de aporte con la locación seleccionada. | Cumple · 0,047 s | Cumple · 0,039 s |
| V-02b | RF-02 | Error | Abrir las locaciones 300 y 3. Una no existe y la otra está inactiva. | HTTP 404 en ambos casos, con aviso y retorno a la portada. | Cumple · 0,022 s | Cumple · 0,016 s |
| V-03a | RF-03 | Correcto | Pedir el original del documento 1, un PDF llamado «ficha histórica.pdf». | HTTP 200 con tipo PDF, nombre original en la cabecera y bytes idénticos. | Cumple · 0,016 s | Cumple · 0,016 s |
| V-03b | RF-03 | Error | Pedir el original del documento 2, cuyo binario está ausente. | HTTP 404 con aviso de descarga no disponible. La aplicación registra el original ausente. | Cumple · 0,025 s | Cumple · 0,022 s |
| V-04a | RF-04 | Correcto | Enviar sin sesión el PDF válido aporte.pdf, con locación 1 y descripción. | La aplicación redirige a la confirmación con un folio único y guarda el aporte como Pendiente. | Cumple · 0,036 s | Cumple · 0,050 s |
| V-04b | RF-04 | Error | Enviar el aporte sin adjunto, con extensión ZIP, con firma PDF inválida y con un adjunto que excede 25 MB. | HTTP 422 o 413 con aviso de archivo inválido. El total de propuestas se conserva. | Cumple · 0,445 s | Cumple · 0,431 s |
| V-05a | RF-05 | Correcto | Iniciar sesión con la cuenta de prueba y su contraseña válida. | HTTP 303 con cookie de sesión. El panel responde HTTP 200 con las propuestas. | Cumple · 0,085 s | Cumple · 0,103 s |
| V-05b | RF-05 | Error | Iniciar sesión con la cuenta de prueba y una contraseña incorrecta. | HTTP 401 sin cookie de sesión. El contador aumenta en uno y el acceso al panel se deniega. | Cumple · 0,103 s | Cumple · 0,104 s |
| V-06a | RF-06 | Correcto | Pasar un aporte a Revisada y aprobarlo como administrador. | El aporte queda Aprobada, aparece en su locación y se crea un solo documento aunque se reintente. | Cumple · 0,201 s | Cumple · 0,256 s |
| V-06b | RF-06 | Error | Rechazar una propuesta Revisada primero con motivo vacío y luego con «Duplicado». | Sin motivo responde HTTP 422 y conserva Revisada. Con motivo queda Rechazada y registra la justificación. | Cumple · 0,189 s | Cumple · 0,242 s |

## RF-07 a RF-12

| Caso | RF | Tipo | Entrada y acción | Resultado esperado | Antes | Después |
|---|---|---|---|---|---|---|
| V-07a | RF-07 | Correcto | Abrir la cronología de una locación con fechas 1999-02, 1995 y un documento sin fecha. | Muestra 1995 antes de 1999-02 y deja fuera el documento sin fecha. | Cumple · 0,125 s | Cumple · 0,127 s |
| V-07b | RF-07 | Error | Abrir la cronología con cero documentos, uno y dos con fechas equivalentes (1995 y 1995-01-01). | La línea del tiempo se oculta y aparece el aviso de fechas insuficientes. | Cumple · 0,119 s | Cumple · 0,117 s |
| V-08a | RF-08 | Correcto | Descargar el ZIP de una locación con dos originales PDF del mismo nombre. | HTTP 200 con ZIP. Contiene ambos originales con bytes idénticos y nombres distinguibles. | Cumple · 0,204 s | Cumple · 0,098 s |
| V-08b | RF-08 | Error | Descargar el ZIP de una locación vacía, con exactamente 100 MB y después con más de 100 MB. | HTTP 200 con los 100 MB completos. Vacío y exceso reciben HTTP 422. El exceso se informa sin cabecera de descarga. | Cumple · 0,664 s | Cumple · 0,907 s |
| V-09a | RF-09 | Correcto | Registrar «Glorieta de Prueba» con coordenadas 21.173 y −86.821 desde una sesión de administración. | HTTP 303 a la ficha. El alta y sus coordenadas aparecen en el catálogo y los datos del mapa. | Cumple · 0,179 s | Cumple · 0,141 s |
| V-09b | RF-09 | Error | Registrar «LOCACIÓN PÚBLICA» con latitud 91 y longitud −181. | HTTP 422 con errores por duplicado y por coordenadas. Se conserva el total de locaciones. | Cumple · 0,171 s | Cumple · 0,151 s |
| V-10a | RF-10 | Correcto | Publicar original.pdf desde curaduría con locación, título, fecha 1997 y descripción. | HTTP 303 a la nueva ficha. El documento figura en su locación y permite descargar el archivo original. | Cumple · 0,174 s | Cumple · 0,184 s |
| V-10b | RF-10 | Error | Publicar desde curaduría con título, descripción y adjunto, dejando vacías la locación y la fecha. | HTTP 422 con errores para los dos campos. Se conservan los totales de documentos y binarios. | Cumple · 0,128 s | Cumple · 0,136 s |
| V-11a | RF-11 | Correcto | Abrir la descripción de un documento con enlaces a una locación y a otro documento activo. | Los enlaces se muestran en la ficha y cada uno redirige al destino indicado. | Cumple · 0,120 s | Cumple · 0,117 s |
| V-11b | RF-11 | Error | Seguir desde una ficha un enlace que apunta a la locación inactiva 3. | HTTP 303 de vuelta a la ficha de origen con aviso de recurso no disponible. | Cumple · 0,057 s | Cumple · 0,061 s |
| V-12a | RF-12 | Correcto | Buscar «publica» por nombre y «otra resena» por descripción. | Las dos búsquedas devuelven solo sus respectivas coincidencias y sus coordenadas para el mapa. | Cumple · 0,028 s | Cumple · 0,027 s |
| V-12b | RF-12 | Error | Buscar «SinCoincidencias» con el filtro previo «Segunda». | Se conserva la locación del filtro previo y aparece el aviso de búsqueda sin resultados. | Cumple · 0,019 s | Cumple · 0,016 s |

## Alcance de la prueba del mapa

Las salidas de Node.js para V-01a y V-01b están en el registro de cada revisión. El ajuste posterior de contingencia de RNF-03 se documenta con su prueba en Chromium. La medición de carga con Google Maps real sigue pendiente de una clave autorizada. [Contingencia en el navegador](verificacion-v2.md#contingencia-en-el-navegador)
