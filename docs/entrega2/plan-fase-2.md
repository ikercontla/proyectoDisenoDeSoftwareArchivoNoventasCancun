# Plan de refactorización · Fase 2

Equipo 03 · Archivo Noventas Cancún · 22 de noviembre de 2026

El punto de partida es [`v1-antes`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/v1-antes), con los 12 requisitos funcionales. La suite Maven pasó 25 pruebas y se verificaron por separado los dos casos del mapa con Google Maps simulado. Los seis Must están disponibles en [`base-must`](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/base-must). [Diagnóstico y métricas iniciales](diagnostico-v1.md) · [Verificación inicial](verificacion-v1.md)

## Cambios propuestos

| Orden | Hallazgo | Técnica principal | Cambio previsto | Casos de regresión |
|---|---|---|---|---|
| R-01 | H-02, DIP | Extract Interface · Generalización | Definir la interfaz `ProveedorMapa` en JavaScript. Su contrato recibe locaciones, entrega la selección de un marcador y comunica errores sin tipos de Google. La vista recibe el proveedor y conserva la ficha, la búsqueda y el catálogo alternativo. La implementación de Google Maps contiene el acceso al SDK. | V-01a/b, V-09a y V-12a/b. Verificar el contrato con un proveedor de prueba que carga y falla. |
| R-02 | H-01, SRP | Move Function | Trasladar la normalización de nombres y la inspección de binarios de `PropuestaAporte` a `Adjunto`. Conservar el límite, las firmas válidas, los errores y la protección de nombres en el ZIP. | V-03a/b, V-04a/b, V-06a/b, V-08a/b y V-10a/b. |
| R-03 | H-06 | Replace Primitive with Object · Organización de datos | Introducir `FechaHistorica` como valor que conserva la precisión de entrada y calcula el primer día del periodo para ordenar. Mantener en SQLite los textos existentes y la diferencia entre fecha opcional en aportes y fecha obligatoria en publicación directa. | V-04a/b, V-06a/b, V-07a/b y V-10a/b. Cubrir año, mes, día inválido y empate de día de orden. |
| R-04 | H-05, OCP | Extract Function · Composición de métodos | Extraer una función de consulta del catálogo de formatos. Alimentar desde ese catálogo la validación de extensiones y las opciones de ambos formularios. Mantener la lista actual, las verificaciones de firma y los mensajes. | V-03a/b, V-04a/b y V-10a/b. Comparar el atributo `accept` en ambos formularios y probar un contenido incompatible. |
| R-05 | H-03, SRP | Extract Class | Extraer de `Pantallas` el controlador de alta de locaciones y publicación directa. Conservar rutas, filtros de sesión, formularios, códigos HTTP y redirecciones. | V-02a/b, V-05a/b, V-06a/b, V-09a/b y V-10a/b. Comprobar que usuarios anónimos se redirigen al acceso. |

La [bitácora](bitacora-refactorizacion.md) registra los cambios aplicados y su evidencia. El orden coloca primero el proveedor externo y luego los ajustes que usan `Adjunto` y el formato de fecha. [Catálogo de técnicas de Fowler](https://refactoring.com/catalog/) · [Extract Function](https://refactoring.com/catalog/extractFunction.html) · [Replace Primitive with Object](https://refactoring.com/catalog/replacePrimitiveWithObject.html)

Tras cerrar los cinco cambios iniciales en `v2-refactorizado`, R-06 atendió H-04 al reunir el acceso a los binarios en `Originales`. El [cierre técnico](cierre-tecnico.md) recoge las métricas, el UML y el escenario DEP-02 de la revisión adicional.

## Verificación y evidencia por cambio

1. Ejecutar `mvnw verify` desde `v1-antes` y reutilizar las entradas y resultados esperados de la [matriz de verificación](verificacion-v1.md).
2. Aplicar cada cambio en su propio commit, con mensaje del tipo `R-01: corrige H-02`. Registrar en la bitácora el hallazgo, técnica, alcance, casos, resultado, SHA y fragmentos antes y después.
3. Después de cada cambio, volver a ejecutar los casos afectados y la suite completa. Registrar también los resultados externos de Google Maps cuando haya una clave autorizada. Repetir la carga del ZIP si cambia su generación o el acceso a originales.
4. Para el escenario de **DEP-01**, simular la sustitución de Google Maps por otro proveedor. En la base, `mapa.js` concentra tanto al proveedor como la interacción, y `Pantallas` y la configuración suministran sus parámetros. En la versión resultante, contar las clases que realmente habría que editar y consignar por separado los módulos JavaScript y archivos de configuración. Verificar con un proveedor simulado que se conservan la selección de marcadores y la contingencia.
5. Actualizar el UML desde el código resultante e identificar clases nuevas o modificadas y el hallazgo que atienden. Recalcular CBO y LCOM sobre las mismas tres clases y comparar los valores en una sola tabla. Completar la reflexión y fijar `v2-refactorizado` tras verificar el resultado.

## Validación externa pendiente

V-01a y V-01b con Google Maps real requieren una clave autorizada. El comportamiento del catálogo ya se observa localmente y el fallo de carga se ejercitó con simulación. Para RNF-03, medir la respuesta ante bloqueo y ante falta de respuesta. El código inicial conserva un temporizador de cinco segundos para el segundo caso. Si la medición requiere corregir un tiempo observable, registrarlo como corrección funcional independiente de la refactorización. [Código inicial del mapa](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/blob/5d152e48257858de55c34d4d4ce5e8b36da68ecb/src/main/resources/static/mapa.js#L9) · [RNF del SRS](../srs/seccion7_requerimientos_no_funcionales.md)

## Uso de IA

OpenAI Codex apoyó la propuesta de secuencia, técnicas y casos de regresión.
