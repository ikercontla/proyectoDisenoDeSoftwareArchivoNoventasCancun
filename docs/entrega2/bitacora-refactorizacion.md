# Bitácora de refactorización · Entrega 2

Equipo 03 · Archivo Noventas Cancún

La comparación parte de [`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes). Los IDs corresponden al [diagnóstico](diagnostico-v1.md) y al [plan de fase 2](plan-fase-2.md).

| ID | Hallazgo | Técnica | Qué cambió | Commit | Casos verificados |
|---|---|---|---|---|---|
| R-01 | H-02 · DIP | Extract Interface · Generalización | Se definió el contrato `ProveedorMapa`. La vista prepara los puntos, las fichas y la contingencia. El módulo de Google concentra la carga del SDK, el encuadre y los eventos del proveedor. | [`90945e9`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/90945e9cc013c5d73e598514127de6d71d3efae3) | V-01a/b con simulación, V-09a y V-12a/b en la suite Java. |

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

## Uso de IA

OpenAI Codex apoyó la implementación de R-01, las pruebas con el proveedor simulado y el registro de resultados.
