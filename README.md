# Archivo Noventas Cancún

Aplicación del Equipo 03 para consultar documentos por locación, recibir aportes ciudadanos y moderarlos. La primera implementación cubre los requisitos Must RF-01 a RF-06 de la [especificación](docs/srs/seccion5_requerimientos_funcionales.md).

## Ejecutar en local

Requiere Node.js 24 o superior. Usa SQLite integrado en Node.js y guarda el catálogo, los binarios y las propuestas en `.data/archivo.sqlite`.

```powershell
npm.cmd run demo
npm.cmd start
```

Abrir [http://127.0.0.1:3000](http://127.0.0.1:3000). La carga de demostración inicializa una base vacía con dos locaciones y un PDF de ejemplo. Para elegir otra base, establecer `ARCHIVO_DB` antes de ejecutar los comandos.

### Administración

Crear una cuenta en la misma base y abrir [Administración](http://127.0.0.1:3000/administracion).

```powershell
$env:ADMIN_USER = 'curador'
$env:ADMIN_PASSWORD = Read-Host 'Contraseña de al menos 12 caracteres' -MaskInput
npm.cmd run admin
Remove-Item Env:ADMIN_PASSWORD
```

Abrir una propuesta pendiente la pasa a `Revisada`. Al aprobarla, el documento aparece en su locación. El título puede indicarlo el curador y, si se omite, se toma el del archivo original. Para publicar una imagen se requiere fecha histórica. Si el aporte llega sin fecha, el curador la puede completar al aprobarlo.

### Mapa

Para habilitar el mapa interactivo, establecer `GOOGLE_MAPS_API_KEY` con una clave de navegador autorizada para Maps JavaScript API y el origen local. Es una clave que el proveedor requiere enviar al navegador. Establecer `GOOGLE_MAPS_MAP_ID` para usar el ID del equipo. Si se omite el ID, se usa `DEMO_MAP_ID`.

El catálogo alternativo sigue disponible cuando falta la configuración o falla la carga del mapa. Para verificar la contingencia, bloquear `maps.googleapis.com` en las herramientas de red del navegador y recargar.

## Verificación

```powershell
npm.cmd test
```

La suite usa una base en memoria y un servidor HTTP local de puerto efímero. Consulta el [registro de la primera ejecución](docs/entrega2/verificacion-v1.md) y el [avance de la Entrega 2](docs/entrega2/avance.md).
