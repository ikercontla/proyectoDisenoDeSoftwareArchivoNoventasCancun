# Primera implementación · Verificación inicial

La primera versión implementa los seis RF Must. Se eligieron Node.js 24 y SQLite para ejecutar y persistir los flujos localmente. Las locaciones y el PDF de demostración son datos de prueba.

## Resultados de `npm test`

| Caso | Resultado inicial | Evidencia de la aserción |
|---|---|---|
| V-01a | Cumple con simulación | El marcador recibe las coordenadas de la locación y su evento muestra una ficha con enlace al archivo. |
| V-01b | Cumple con simulación | El error de carga oculta el mapa y conserva el catálogo con las coordenadas de las locaciones activas. |
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

Se revisaron en el navegador local el catálogo alternativo, la ficha con el enlace de descarga y el formulario con la locación preseleccionada. Queda pendiente ejecutar V-01a y V-01b con una clave autorizada de Google Maps y registrar el tiempo de contingencia de RNF-03. La etiqueta `v1-antes` queda pendiente de esa validación. La comparación posterior se completará tras la refactorización.

## Uso de IA

Se usó OpenAI Codex para implementar la base a partir del SRS, preparar la suite de aceptación y revisar los resultados en consola y navegador.
