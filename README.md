# Project AI-901

API REST para la gestión comercial de una distribuidora de productos de limpieza, con autenticación JWT, inventario, órdenes, tickets, análisis inteligente de facturas y asistentes conversacionales con Azure AI.

## Características

- Gestión de sucursales, almacenes, categorías, productos, inventario, clientes y órdenes.
- Gestión de tickets con asignación, estados, mensajes y calificación.
- Usuarios y roles con autenticación JWT y contraseñas BCrypt.
- Análisis de facturas PDF/JPG/PNG con OCR, deduplicación SHA-256, validaciones y scoring de sospecha.
- Almacenamiento local o Azure Blob Storage.
- Chat interno de inventario y asistente público con Azure AI Agents / Microsoft Foundry, incluido procesamiento de voz.
- Generación de informes de ventas en formato Word con descarga temporal.
- Respuestas uniformes y errores RFC 7807.
- Pruebas con H2 en modo MySQL.

## Stack

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| SQL Server / Azure SQL | Compatible |
| Maven Wrapper | 3.10.0 |
| JJWT | 0.12.6 |
| Azure Blob Storage | 12.36.0 |
| Azure AI Agents | 2.2.0 |
| Azure Document Intelligence | 1.0.11 |
| PDFBox | 3.0.8 |

## Requisitos

- Java 25
- SQL Server o Azure SQL
- Maven Wrapper incluido en el repositorio
- Opcional: cuenta de Azure con Blob Storage, Document Intelligence y Microsoft Foundry

## Inicio rápido

1. Configurar la conexión a SQL Server o Azure SQL mediante
   `SPRING_DATASOURCE_URL`.

```bash
# Ejemplo de formato:
# jdbc:sqlserver://host:1433;databaseName=project_ai901;encrypt=true;trustServerCertificate=true
```

> `docker-compose.yml` conserva una configuración de MySQL para desarrollo
> histórico, pero la aplicación actual utiliza el controlador de SQL Server.
> Antes de usar Docker, la URL, el controlador y el motor deben estar
> alineados.

2. Crear el archivo `.env` a partir de las variables documentadas abajo.

3. Arrancar la aplicación:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```


## Configuración

La aplicación carga opcionalmente `./.env` mediante `spring.config.import`.

### Variables principales

| Variable | Descripción | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de SQL Server/Azure SQL | — |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | — |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | — |
| `JWT_SECRET` | Secreto JWT, mínimo 32 caracteres | efímero |
| `APP_STORAGE_TYPE` | `local` o `azure` | `local` |
| `APP_STORAGE_LOCAL_PATH` | Ruta local de archivos | `./data/storage` |
| `AZURE_STORAGE_CONNECTION_STRING` | Conexión Azure Blob | — |
| `AZURE_STORAGE_CONTAINER_IN` | Contenedor de entrada | `facturas-entrada` |
| `AZURE_STORAGE_CONTAINER_VALID` | Contenedor de válidas | `facturas-validas` |
| `AZURE_STORAGE_CONTAINER_QUARANTINE` | Contenedor de cuarentena | `facturas-cuarentena` |
| `AZURE_OPENAI_ENDPOINT` | Endpoint de Microsoft Foundry | — |
| `AZURE_OPENAI_DEPLOYMENT` | Deployment del modelo | — |
| `AZURE_SPEECH_ENDPOINT` | Endpoint de Azure Speech | — |
| `AZURE_SPEECH_KEY` | Clave de Azure Speech | — |
| `AZURE_SPEECH_LOCALE` | Idioma de transcripción | `es-MX` |
| `AZURE_OPENAI_AGENT_NAME` | Nombre de agente heredado/configurable | — |
| `APP_DI_PROVIDER` | `mock` o `azure` | `mock` |
| `APP_DI_MOCK_MODE` | `sample` o `empty` | `sample` |
| `AZURE_DOC_INTELLIGENCE_ENDPOINT` | Endpoint Document Intelligence | — |
| `AZURE_DOC_INTELLIGENCE_KEY` | Key Document Intelligence | — |

### Reglas de facturas

| Propiedad | Default |
|---|---|
| Tamaño máximo | 10 MB |
| Extensiones | `pdf`, `jpg`, `jpeg`, `png` |
| Páginas máximas PDF | 5 |
| Imagen mínima | 800x600 |
| Fecha antigua | 90 días |
| Tolerancia de total | 0.01 |
| Score de sospecha alto | 3 |
| Confianza baja | 0.80 |

## Seguridad y roles

Roles:

- `BOSS`
- `ADMIN`
- `BRANCH_MANAGER`
- `WAREHOUSE_HEAD`
- `ADMIN_SUPPORT`

Protección actual:

| Ruta | Acceso |
|---|---|
| `POST /auth/login` | Público |
| `GET /auth/me` | Autenticado |
| `POST /invoice-analyzer/{supplierId}` | `ADMIN`, `BOSS`, `ADMIN_SUPPORT` |
| `POST /invoice/{id}/review` | `ADMIN`, `BOSS`, `ADMIN_SUPPORT` |
| `PUT /ticket/{id}/assign` | `ADMIN`, `BOSS`, `ADMIN_SUPPORT`, `BRANCH_MANAGER` |
| `/supplier/**` | Autenticado |
| `/user/**`, `/role/**` | `ADMIN`, `BOSS` |
| `POST /chat/public` | Público |
| `POST /chat` | Autenticado |
| `POST /public/invoice-analyzer/{supplierId}` | `ADMIN`, `BOSS`, `ADMIN_SUPPORT` o clave `X-Public-Upload-Key` válida |
| Resto de endpoints | Autenticado |

## Endpoints

### Autenticación

| Método | Ruta |
|---|---|
| POST | `/auth/login` |
| GET | `/auth/me` |

### Sucursales

| Método | Ruta |
|---|---|
| GET | `/branch` |
| GET | `/branch/{id}` |
| POST | `/branch` |
| PUT | `/branch/{id}` |
| DELETE | `/branch/{id}` |

### Categorías

| Método | Ruta |
|---|---|
| GET | `/category` |
| GET | `/category/{id}` |
| POST | `/category` |
| PUT | `/category/{id}` |
| DELETE | `/category/{id}` |

### Clientes

| Método | Ruta |
|---|---|
| GET | `/customer` |
| GET | `/customer/{id}` |
| POST | `/customer` |
| PUT | `/customer/{id}` |
| DELETE | `/customer/{id}` |

### Almacenes

| Método | Ruta |
|---|---|
| GET | `/warehouse` |
| GET | `/warehouse/{id}` |
| POST | `/warehouse` |
| PUT | `/warehouse/{id}` |
| DELETE | `/warehouse/{id}` |

### Productos

| Método | Ruta |
|---|---|
| GET | `/product` |
| GET | `/product/{id}` |
| POST | `/product` |
| PUT | `/product/{id}` |
| DELETE | `/product/{id}` |

### Proveedores

| Método | Ruta |
|---|---|
| GET | `/supplier` |
| GET | `/supplier/{id}` |
| POST | `/supplier` |
| PUT | `/supplier/{id}` |
| DELETE | `/supplier/{id}` |

### Inventario

| Método | Ruta |
|---|---|
| GET | `/inventory` |
| GET | `/inventory/{id}` |
| POST | `/inventory` |
| PUT | `/inventory/{id}` |
| DELETE | `/inventory/{id}` |

### Órdenes

| Método | Ruta |
|---|---|
| GET | `/order` |
| GET | `/order/{id}` |
| POST | `/order` |
| PUT | `/order/{id}` |
| PUT | `/order/{id}/status` |
| DELETE | `/order/{id}` |

### Tickets

| Método | Ruta |
|---|---|
| GET | `/ticket` |
| GET | `/ticket/{id}` |
| POST | `/ticket` |
| PUT | `/ticket/{id}` |
| PUT | `/ticket/{id}/assign` |
| PUT | `/ticket/{id}/status` |
| PUT | `/ticket/{id}/rating` |
| DELETE | `/ticket/{id}` |
| GET | `/ticket/{id}/message` |
| POST | `/ticket/{id}/message` |

### Usuarios y roles

| Método | Ruta |
|---|---|
| GET | `/user` |
| GET | `/user/{id}` |
| POST | `/user` |
| PUT | `/user/{id}` |
| PUT | `/user/{id}/password` |
| PUT | `/user/{id}/activate` |
| DELETE | `/user/{id}` |
| GET | `/role` |

### Facturas

| Método | Ruta |
|---|---|
| GET | `/invoice` |
| GET | `/invoice/{id}` |
| POST | `/invoice/{id}/review` |
| GET | `/invoice/{id}/file-url` |
| POST | `/invoice-analyzer/{supplierId}` |
| POST | `/public/invoice-analyzer/{supplierId}` |

El análisis de facturas recibe `multipart/form-data` y el campo obligatorio
`file`. El endpoint autenticado devuelve el análisis completo; el endpoint
público devuelve una respuesta limitada para cargas autorizadas.

### Informes

| Método | Ruta |
|---|---|
| POST | `/report/sales` |
| GET | `/report/download/{token}` |

`POST /report/sales` genera un documento Word con las ventas por sucursal y
periodo. El token de descarga es temporal y está asociado al usuario que
generó el informe.

### Chat

| Método | Ruta |
|---|---|
| POST | `/chat` (JSON o multipart/form-data) |
| POST | `/chat/public` (JSON o multipart/form-data) |

## Flujo de facturas

1. Validación de archivo, extensión, tamaño, páginas y dimensiones mínimas.
2. Cálculo de SHA-256 y detección de duplicados.
3. Conversión a PDF si es imagen.
4. Subida al contenedor de entrada.
5. OCR con Document Intelligence o mock.
6. Scoring de sospecha.
7. Clasificación `VALID`, `SUSPICIOUS` o `REJECTED`.
8. Movimiento a contenedor final y registro de eventos.
9. Revisión manual mediante `POST /invoice/{id}/review`.

## Chat con IA

- `/chat` usa el agente de inventario con contexto del usuario autenticado.
- `/chat/public` usa el asistente público sin contexto de usuario.
- Las herramientas se implementan mediante `AgentTool` y se convierten en funciones mediante `ToolRegistry`.
  Incluyen búsqueda de productos, inventario, sucursales, almacenes, pedidos,
  tickets e informes de ventas.
- Los mensajes pueden enviarse por texto o audio; el audio se transcribe con
  Azure Speech antes de procesarse.
- La publicación de agentes se controla con `foundry.agent.publish`.

## Pruebas

```bash
# Linux / macOS
./mvnw test

# Windows PowerShell
.\mvnw.cmd test
```

Las pruebas usan H2 en modo MySQL, `ddl-auto=create-drop` y el perfil `test`. Incluyen pruebas de controladores, servicios, repositorios y flujos end-to-end.

## Formato de respuesta

Éxito:

```json
{
  "timestamp": "2026-10-26T12:00:00Z",
  "status": 200,
  "message": "Operación realizada correctamente",
  "data": {}
}
```

Error:

```json
{
  "type": "about:blank",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Producto no encontrado: ..."
}
```

## Estructura del proyecto

```text
src/main/java/info/rio/project_ai901
├── api/
├── aiTools/
├── config/
├── exceptions/
├── models/
├── repositories/
└── services/
```
