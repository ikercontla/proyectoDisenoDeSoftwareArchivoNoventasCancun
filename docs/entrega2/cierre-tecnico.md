# Cierre técnico · Entrega 2

Equipo 03 · Archivo Noventas Cancún · 24 de noviembre de 2026

[`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes) fija el código inicial de los 12 requisitos funcionales. [`v2-refactorizado`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v2-refactorizado) reúne R-01 a R-05. [`v2.1-refactorizado`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v2.1-refactorizado) incorpora R-06 para H-04, con su evidencia y diagrama actualizados. Cada refactorización tiene un commit propio. [Bitácora y fragmentos antes y después](bitacora-refactorizacion.md) · [Resultados de verificación](verificacion-v2.md)

## Comparación de métricas

Se midieron las mismas tres clases de las tarjetas CRC, con el [criterio de conteo inicial](diagnostico-v1.md). CBO reúne las dependencias de clases en ambas direcciones. LCOM cuenta los pares de métodos con y sin atributos de instancia compartidos.

| Clase | CBO antes | CBO después | LCOM antes | LCOM después | Interpretación |
|---|---:|---:|---:|---:|---|
| `PropuestaAporte` | 6 | **4** | 21 | **1** | Salen `Adjunto`, `Curaduria` y `PaquetesZip` del conjunto de dependencias. Se incorpora `FechaHistorica`. La clase conserva la lectura del registro y su conversión a publicación. |
| `Archivo` | 9 | **11** | 0 | **0** | Usa `FechaHistorica` en la persistencia y recibe llamadas desde `PantallasCuraduria`. R-06 sustituye la referencia a `Archivo.Original` por `Originales` y reduce de diez a siete sus métodos contabilizados. |
| `Documento` | 7 | **8** | 6 | **4** | La fecha pasa a `FechaHistorica`. El aumento de CBO refleja esa dependencia explícita. La menor LCOM coincide con el traslado del cálculo del día de orden. |

### Cálculo de CBO después

| Clase | Usa | La usan | Total |
|---|---|---|---:|
| [`PropuestaAporte`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/main/java/mx/archivonoventas/PropuestaAporte.java) | `Documento`, `FechaHistorica` | `Pantallas`, `Propuestas` | 4 |
| [`Archivo`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/48c1ea64be8fa20d5c0c04675df03ff50d8f67eb/src/main/java/mx/archivonoventas/Archivo.java) | `JdbcTemplate`, `Locacion`, `Documento`, `Originales`, `ClassPathResource`, `FechaHistorica` | `Aplicacion`, `Curaduria`, `Pantallas`, `PantallasCuraduria`, `Propuestas` | 11 |
| [`Documento`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/main/java/mx/archivonoventas/Documento.java) | `TextoVinculado`, `FechaHistorica` | `Archivo`, `PropuestaAporte`, `Curaduria`, `Cronologia`, `Pantallas`, `Propuestas` | 8 |

### Cálculo de LCOM después

| Clase | Métodos y atributos propios | P | Q | LCOM |
|---|---|---:|---:|---:|
| `PropuestaAporte` | `leer` usa cero atributos. `paraPublicar` usa `nombre`, `locacionId`, `binarioId`, `descripcion`, `fechaHistorica` y `tipo`. La única pareja no comparte atributos. | 1 | 0 | 1 |
| `Archivo` | `locaciones`, `locacion`, `documentos`, `documentoPublicado`, `incorporar` y `cargarDemo` usan `db`. `cargarDemo` también usa `originales`. `leerLocacion` usa cero atributos. Hay 15 parejas con `db` compartido y seis parejas con el lector. | 6 | 15 | 0 |
| `Documento` | `leer` usa cero atributos. `validarPublicacion` usa `titulo`, `tipo`, `fechaHistorica` y `descripcion`. `referenciaDescarga` usa `id`. `resumen` usa `descripcion`. De las seis parejas, una comparte `descripcion`. | 5 | 1 | 4 |

## Diagrama refactorizado

El [fuente PlantUML](../../diagramas/Equipo03_Entrega2_DiagramaClases.puml) incluye cuatro vistas. Sus colores marcan en verde los elementos nuevos y en azul los modificados desde `v1-antes`. El triángulo discontinuo indica que el objeto que fabrica el módulo de Google cumple el contrato de JavaScript `ProveedorMapa`.

| Elementos nuevos o modificados | Hallazgo y refactorización | RF o RN que atienden |
|---|---|---|
| `ProveedorMapa`, `SolicitudMapa`, `PuntoDelMapa`, `mapa-vista.js`, `mapa-google.js` y `mapa.js` | H-02, R-01 | RF-01, RF-09 y RF-12. Puntos del catálogo, fichas y contingencia. |
| `Adjunto`, `PaquetesZip` y `PropuestaAporte` | H-01, R-02 | RF-04, RF-08, RF-10 y RN-03. Validación de carga, saneamiento en el ZIP y propuesta. |
| `FechaHistorica`, `Documento`, `Archivo`, `PropuestaAporte`, `Propuestas`, `Curaduria` y `Cronologia` | H-06, R-03 | RF-04, RF-06, RF-07, RF-10, RN-02 y RN-04. Captura, persistencia y orden de las fechas con precisión parcial. |
| `Adjunto.FormatoOriginal`, `Adjunto` y `Pantallas` | H-05, R-04 | RF-04, RF-10 y RN-02. Firmas, extensiones y ayudas de carga. |
| `PantallasCuraduria`, `RespuestaWeb` y `Pantallas` | H-03, R-05 | RF-04, RF-09, RF-10, RN-01 y RN-02. Flujo de curaduría, altas, publicación y respuestas HTTP. |
| `Originales`, sus tipos de transferencia y lectura, `PaquetesZip`, `Archivo`, `Curaduria`, `Propuestas` y `Pantallas` | H-04, R-06 | RF-03, RF-04, RF-06, RF-08 y RF-10. Acceso a originales en descargas, aportes, aprobación y publicación directa. |

El [diseño conceptual de la Entrega 1](../../diagramas/Equipo3_Entrega1_DiagramaClases.puml) modelaba `DocumentoPDF` e `Imagen` por separado y asignaba la cronología y el ZIP a `Archivo`. El código implementado usa `Documento` con el tipo y el identificador del binario, y delega cronología y compresión a `Cronologia` y `PaquetesZip`. El diagrama de la Entrega 2 dibuja esos servicios y muestra las fechas, la validación de adjuntos y los controladores en la forma que tienen en la aplicación.

## Prueba de cambio

| Escenario | Origen | Clases a modificar antes | Clases a modificar después |
|---|---|---|---|
| El acervo pasa a admitir un año aproximado escrito como `ca. 1995`. Tiene que conservar el prefijo al presentar y persistir el dato, y ordenar en el primer día del año de referencia. | SUP-01 | **2**. `PropuestaAporte` debe aceptar y validar el prefijo, mientras `Documento` debe reconocerlo para el orden cronológico. | **1**. `FechaHistorica` centraliza la interpretación y el día de orden. Los flujos de ingreso y cronología usan sus operaciones existentes. |
| Se cambia la implementación que escribe, busca y lee el contenido binario, conservando la identidad y los metadatos de cada original. | DEP-02 | **2**. `Archivo` y `PaquetesZip` escriben o leen el contenido. | **1**. `Originales` concentra las operaciones sobre el contenido. |

Se rastrearon las llamadas desde los formularios hasta la persistencia y el orden cronológico. Si se actualiza la ayuda para enseñar el nuevo formato, se edita la misma plantilla en ambas revisiones. El escenario mide el alcance del cambio. El formato vigente conserva año, año-mes y fecha completa. [SUP-01](../srs/seccion4_supuestos_dependencias.md) · [Antes](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/v1-antes/src/main/java/mx/archivonoventas/PropuestaAporte.java#L51) · [Después](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/main/java/mx/archivonoventas/FechaHistorica.java)

Para DEP-01, el caso de contrato sustituyó el proveedor de Google por un doble que recibió los marcadores y comunicó carga y fallo. La vista conservó sus fichas y el catálogo. Ese ensayo comprueba el punto de sustitución de R-01 en el navegador. [Pruebas del mapa](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/f2dcba8e4257ce94fe83264a4b536ea66003ab78/src/test/js/mapa.test.mjs)

Para DEP-02 se rastrearon la escritura, la descarga individual y la generación del ZIP. El conteo considera cambios al acceso del contenido. Una migración de datos o de transacciones requeriría su propia revisión. [DEP-02](../srs/seccion4_supuestos_dependencias.md) · [Cambio de acceso a originales](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/commit/48c1ea64be8fa20d5c0c04675df03ff50d8f67eb)

## Variante descartada

Se ensayó en una copia aislada guardar únicamente el día de orden. La prueba de precisión esperaba `1995` y obtuvo `1995-01-01`. Se conservó el texto original para distinguir un año conocido de una fecha completa. Esta decisión preserva los datos históricos y satisface SUP-01.
