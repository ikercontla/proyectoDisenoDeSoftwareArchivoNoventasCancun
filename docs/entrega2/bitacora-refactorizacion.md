# Bitácora de refactorización · Entrega 2

Equipo 03 · Archivo Noventas Cancún

La comparación parte de [`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes). Los IDs corresponden al [diagnóstico](diagnostico-v1.md) y al [plan de fase 2](plan-fase-2.md).

| ID | Hallazgo | Técnica | Qué cambió | Commit | Casos verificados |
|---|---|---|---|---|---|
| R-01 | H-02 · DIP | Extract Interface · Generalización | Se definió el contrato `ProveedorMapa`. La vista prepara los puntos, las fichas y la contingencia. El módulo de Google concentra la carga del SDK, el encuadre y los eventos del proveedor. | [`90945e9`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/90945e9cc013c5d73e598514127de6d71d3efae3) | V-01a/b con simulación, V-09a y V-12a/b en la suite Java. |
| R-02 | H-01 · SRP | Move Function | Se trasladaron el saneamiento de nombres y la verificación de firmas a `Adjunto`. El ZIP reutiliza el mismo saneamiento. | [`69cd118`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/69cd11848913bd24c0e65782f48d9a55133dfd82) | V-03a/b, V-04a/b, V-06a/b, V-08a/b, V-10a/b. |
| R-03 | H-06 | Replace Primitive with Object · Organización de datos | `FechaHistorica` conserva el texto y calcula el día de orden. SQLite sigue guardando las mismas cadenas. | [`1951465`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/19514655527547283b1d308895a45bd87befc046) | V-04a/b, V-06a/b, V-07a/b, V-10a/b y dos pruebas de fechas. |
| R-04 | H-05 · OCP | Extract Function · Composición de métodos | El catálogo de formatos alimenta las firmas, el atributo `accept`, la ayuda y el mensaje de error. | [`e88f6f6`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/e88f6f61dad25a750586fbe66552f1787cba1a8a) | V-03a/b, V-04a/b, V-10a/b, ambas vistas y las cuatro firmas. |
| R-05 | H-03 · SRP | Extract Class | Las altas y publicaciones directas pasan a `PantallasCuraduria`. `RespuestaWeb` concentra la preparación de vistas, redirecciones y errores compartidos. | [`f2dcba8`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/f2dcba8e4257ce94fe83264a4b536ea66003ab78) | V-02a/b, V-05a/b, V-06a/b, V-09a/b, V-10a/b y carga sobredimensionada. |

## R-01 · Aislar el proveedor de mapas

### Antes

El acceso al SDK y la lectura de locaciones del catálogo ocurrían en la misma función de inicialización.

```javascript
const maps = window.google.maps;
const fichas = [...document.querySelectorAll('.lista-locaciones li')];
```

[Código anterior](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/5d152e48257858de55c34d4d4ce5e8b36da68ecb/src/main/resources/static/mapa.js#L23)

### Después

El módulo de entrada crea el proveedor y lo entrega a la vista. La vista usa el contrato `ProveedorMapa` y recibe eventos de montaje, carga y fallo.

```javascript
const proveedor = crearProveedorGoogle({ clave: panel.dataset.key, idMapa: panel.dataset.mapId });
prepararMapa(panel, document.querySelector('[data-estado-mapa]'), proveedor);
```

[Modo de entrada](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/90945e9cc013c5d73e598514127de6d71d3efae3/src/main/resources/static/mapa.js#L4) · [Contrato y vista](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/90945e9cc013c5d73e598514127de6d71d3efae3/src/main/resources/static/mapa-vista.js#L17) · [Implementación de Google Maps](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/90945e9cc013c5d73e598514127de6d71d3efae3/src/main/resources/static/mapa-google.js)

### Resultado de las verificaciones

| Caso | Antes | Después |
|---|---|---|
| V-01a | Coordenadas y ficha comprobadas con Google Maps simulado. | El proveedor simulado recibe los puntos del catálogo y la implementación de Google abre la ficha del marcador. |
| V-01b | La simulación de fallo oculta el mapa y muestra el catálogo. | El catálogo queda visible al ocurrir un error, una desconexión o al cumplirse el tiempo de espera. Los eventos tardíos conservan ese estado. |
| V-09a | Un alta autenticada publica la locación y sus coordenadas aparecen en la página. | La prueba HTTP vuelve a comprobar el alta y los datos del marcador. |
| V-12a/b | La búsqueda filtra por nombre y reseña. Si falla, conserva el filtro anterior. | Las pruebas HTTP confirman ambos resultados. El proveedor simulado recibe el conjunto visible. |

La compilación de R-01 con Java 17 pasó las 25 pruebas de Maven. Las siete pruebas del mapa en Node.js 24 pasaron con dobles del navegador y del SDK.

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
tests 7, pass 7, fail 0
```

V-01a y V-01b con Google Maps real siguen pendientes de una clave autorizada. Se conserva la espera de cinco segundos documentada para la falta de respuesta. [Evidencia inicial](verificacion-v1.md) · [Pruebas del mapa](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/90945e9cc013c5d73e598514127de6d71d3efae3/src/test/js/mapa.test.mjs)

## R-02 · Trasladar la validación de adjuntos

**Antes.** `Adjunto.leer` delegaba la inspección de archivos a la clase que representa una propuesta.

```java
return new Adjunto(nombre, PropuestaAporte.tipoArchivo(nombre, datos), datos);
```

**Después.** `Adjunto.leer` utiliza la validación de su propia clase. Se conservaron las firmas, los mensajes y el límite de carga.

```java
return new Adjunto(nombre, tipoArchivo(nombre, datos), datos);
```

Maven pasó 25 pruebas y el mapa simulado pasó 7. [Adjunto antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/90945e9cc013c5d73e598514127de6d71d3efae3/src/main/java/mx/archivonoventas/Adjunto.java) · [Adjunto después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/69cd11848913bd24c0e65782f48d9a55133dfd82/src/main/java/mx/archivonoventas/Adjunto.java) · [Saneamiento en ZIP](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/69cd11848913bd24c0e65782f48d9a55133dfd82/src/main/java/mx/archivonoventas/PaquetesZip.java)

## R-03 · Modelar la fecha histórica

**Antes.** `PropuestaAporte` normalizaba la entrada y `Documento` volvía a interpretar la longitud de la cadena para ordenar.

```java
String fecha = normalizarFecha(fechaPropuesta);
```

**Después.** `FechaHistorica` reúne la validación, el texto que se guarda y la conversión usada por la cronología.

```java
FechaHistorica fecha = FechaHistorica.desdeEntrada(fechaPropuesta);
```

Maven pasó 27 pruebas y el mapa simulado pasó 7. Las dos pruebas de fechas incluyen precisión parcial, año bisiesto, fechas inválidas y entrada vacía. [Propuesta antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/69cd11848913bd24c0e65782f48d9a55133dfd82/src/main/java/mx/archivonoventas/PropuestaAporte.java) · [Propuesta después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/19514655527547283b1d308895a45bd87befc046/src/main/java/mx/archivonoventas/PropuestaAporte.java) · [Fecha histórica](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/19514655527547283b1d308895a45bd87befc046/src/main/java/mx/archivonoventas/FechaHistorica.java)

## R-04 · Compartir el catálogo de formatos

**Antes.** El clasificador y los formularios enumeraban las extensiones en sitios distintos.

```html
accept=".jpg,.png,.webp,.pdf"
```

**Después.** La lista de extensiones de los formularios proviene del mismo catálogo que valida las firmas.

```java
static String extensionesAceptadas() {
    return FORMATOS.stream().map(FormatoOriginal::extension).collect(Collectors.joining(","));
}
```

Maven pasó 30 pruebas y el mapa simulado pasó 7. Se comprobaron los dos atributos `accept`, las cuatro firmas y el rechazo de una firma incompatible. [Adjunto antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/19514655527547283b1d308895a45bd87befc046/src/main/java/mx/archivonoventas/Adjunto.java) · [Adjunto después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/e88f6f61dad25a750586fbe66552f1787cba1a8a/src/main/java/mx/archivonoventas/Adjunto.java) · [Formularios](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/e88f6f61dad25a750586fbe66552f1787cba1a8a/src/main/resources/templates/pantallas.html)

## R-05 · Extraer las pantallas de curaduría

**Antes.** El controlador `Pantallas` contenía las rutas públicas, la sesión y las operaciones de alta y publicación.

```java
@Controller
class Pantallas {
```

**Después.** `PantallasCuraduria` recibe las rutas de alta y publicación. El filtro de autenticación y la respuesta ante una carga excesiva se comprobaron por HTTP.

```java
@Controller
class PantallasCuraduria {
```

Maven pasó 31 pruebas y el mapa simulado pasó 7. La publicación de un binario que supera 25 MB responde 413, conserva el formulario de curaduría y mantiene el conteo de documentos. [Controlador antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/e88f6f61dad25a750586fbe66552f1787cba1a8a/src/main/java/mx/archivonoventas/Pantallas.java) · [Controlador después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/main/java/mx/archivonoventas/PantallasCuraduria.java) · [Respuestas compartidas](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/main/java/mx/archivonoventas/RespuestaWeb.java)

[Verificación comparativa de los 24 casos](verificacion-v2.md) · [Métricas, UML y prueba de cambio](cierre-tecnico.md)

## Uso de IA

OpenAI Codex apoyó la implementación de R-01, las pruebas con el proveedor simulado y el registro de resultados.
