# Archivo Noventas Cancún

Aplicación del Equipo 03 para consultar documentos por locación, recibir aportes ciudadanos y moderarlos. Esta rama cubre los 12 requisitos funcionales de la especificación. La etiqueta `base-must` conserva el commit con los seis requisitos Must. [Especificación funcional](docs/srs/seccion5_requerimientos_funcionales.md) · [Base Must en GitHub](https://github.com/ikercontla/proyectoDisenoDeSoftwareArchivoNoventasCancun/tree/base-must)

## Entrega 2

El PDF, el diagrama de clases editable y renderizado, y la evidencia de los 24 casos se reúnen en la [sección de entrega final](docs/entrega2/README.md).

## Ejecutar en local

Requiere JDK 17 o superior. El servidor usa Java y Spring Boot, con plantillas HTML, CSS y SQLite. Maven Wrapper descarga Maven y las dependencias en la primera compilación.

```powershell
.\mvnw.cmd verify
java -jar target/archivo-noventas-cancun-1.0.0.jar demo
java -jar target/archivo-noventas-cancun-1.0.0.jar
```

En macOS y Linux, usar `./mvnw verify` para compilar. Abrir [http://127.0.0.1:3000](http://127.0.0.1:3000). La carga de demostración inicializa una base vacía con dos locaciones y dos PDF de ejemplo para explorar la cronología, el ZIP y los enlaces.

El catálogo, los binarios, las propuestas y las cuentas se guardan en `.data/archivo.sqlite`. El servidor Java puede abrir la base de la versión anterior. Para usar otra base, establecer `ARCHIVO_DB` antes de iniciar la aplicación o ejecutar cualquiera de los comandos de configuración.

### Administración

Crear una cuenta en la misma base y abrir [Administración](http://127.0.0.1:3000/administracion).

```powershell
$env:ADMIN_USER = 'curador'
$env:ADMIN_PASSWORD = Read-Host 'Contraseña de al menos 12 caracteres' -MaskInput
java -jar target/archivo-noventas-cancun-1.0.0.jar admin
Remove-Item Env:ADMIN_PASSWORD
```

Abrir una propuesta pendiente la pasa a `Revisada`. Al aprobarla, el documento aparece en su locación. El título puede indicarlo el curador y, si se omite, se toma el del archivo original. Para publicar una imagen se requiere fecha histórica. Si el aporte llega sin fecha, el curador la puede completar al aprobarlo.

Desde el panel también se puede registrar una locación o publicar un documento directamente. Para enlazar un documento o locación en una descripción, usar `[Ver documento](/documentos/1)` o `[Ver locación](/locaciones/2)` con el identificador correspondiente.

### Mapa

Para habilitar el mapa interactivo, establecer `GOOGLE_MAPS_API_KEY` con una clave de navegador autorizada para Maps JavaScript API y el origen local. Es una clave que el proveedor requiere enviar al navegador. Establecer `GOOGLE_MAPS_MAP_ID` para usar el ID del equipo. Si se omite el ID, se usa `DEMO_MAP_ID`.

El navegador usa un script de JavaScript para cargar Google Maps y mostrar las fichas al seleccionar marcadores. El catálogo alternativo sigue disponible cuando falta la configuración o falla la carga del mapa. Para verificar la contingencia, bloquear `maps.googleapis.com` en las herramientas de red del navegador y recargar.

## Verificación

```powershell
.\mvnw.cmd test
```

La suite usa una base SQLite temporal y un servidor HTTP local de puerto efímero.

Para ejecutar también las pruebas del proveedor de mapas simulado, usar Node.js 24 con `node --test src/test/js/mapa.test.mjs`.
