# MCmovil Care

## Descripcion del Proyecto
Aplicacion movil nativa para Android desarrollada en Kotlin para la empresa MCmovil SpA. Este MVP centraliza la gestion de ordenes de trabajo, diagnosticos tecnicos, autorizacion de presupuestos y garantias de soporte de hardware, operando de forma independiente al sistema interno Gestioo.

El proyecto implementa el patron de arquitectura MVVM (Model-View-ViewModel), combinando vistas en XML y Jetpack Compose, persistencia local con Room (SQLite) y consumo de API REST con Retrofit.

## Integrantes
* Daniel Ignacio Alday Troncoso
* Emanuel Castillo

## Funcionalidades Implementadas
* **Autenticacion y Roles:** Inicio de sesion con correo/contrasena y cuenta de Google (Credential Manager), persistencia de sesion mediante `SharedPreferences` y redireccion automatica segun tres perfiles (`Cliente`, `Tecnico`, `Admin`).
* **Perfil Cliente:** Visualizacion reactiva de equipos en reparacion, revision de detalle de ordenes, aprobacion o rechazo de presupuestos y descarga de orden de servicio en formato PDF.
* **Perfil Tecnico:** Registro de nuevos equipos mediante lectura de codigos QR/SKU con camara (Google ML Kit) y actualizacion en tiempo real de diagnosticos, montos y estados de las ordenes.
* **Perfil Administrador:** Creacion de nuevos usuarios con asignacion de rol y supervision global del historial de ordenes.
* **Persistencia Local (Room):** Almacenamiento en SQLite con sincronizacion reactiva mediante `Flow` y corrutinas para tablas de Usuarios, Equipos y Ordenes de Trabajo.
* **Recursos Nativos:** Solicitud de permisos en tiempo de ejecucion, notificaciones locales en la barra de estado, escaneo con camara y generacion nativa de documentos PDF.
* **Conexion a Red:** Consumo y sincronizacion de API REST configurada mediante Retrofit y Gson.

## Endpoints API REST (MockAPI)
* **URL Base:** `https://6af9d9cf6d8b86fb22d74b96.mockapi.io/`
  * `GET /ordenes` — Lista todas las ordenes de trabajo.
  * `POST /ordenes` — Crea una nueva orden de trabajo.
  * `PUT /ordenes/{id}` — Actualiza estado, diagnostico o autorizacion de una orden.
  * `GET /usuarios` — Lista los usuarios registrados.
  * `POST /usuarios` — Registra un nuevo usuario.

## Pasos para Ejecutar
1. Abrir Android Studio y seleccionar **Clone** (o **Get from VCS**).
2. Copiar la URL `https://github.com/MemaVE18/MCmovilCare1.0.git`, pegarla en el apartado de clonacion y seleccionar la carpeta de destino.
3. Esperar a que Gradle descargue las dependencias y termine la sincronizacion (**Sync Project with Gradle Files**).
4. Configurar un emulador o conectar un dispositivo fisico con **Android 7.0 (API 24)** o superior.
5. Presionar el boton **Run 'app'** (`Shift + F10`) en Android Studio.
6. Para pruebas rapidas en el login sin crear usuarios previos, ingresar con:
   * **Admin:** `admin@duoc.cl` / `1234`
   * **Tecnico:** `tecnico@duoc.cl` / `1234`
   * **Tecnico:** `emacast.tecnico@duoc.cl` / `123`
   * **Cliente:** `cliente@duoc.cl` / `1234`

## Testing
Con un emulador o dispositivo Android encendido:
1. En el panel izquierdo de Android Studio (vista **Android**), dirigirse a `app > java > com.example.mcmovilcare (androidTest)`.
2. Hacer clic derecho sobre la carpeta `com.example.mcmovilcare (androidTest)` y seleccionar **Run 'Tests in 'com.example.mcmovilcare''**.
3. Esperar los resultados en el panel inferior **Run** (40 pruebas automatizadas).

## Visualizar Cobertura del Testing
1. Con el emulador encendido, abrir la pestana **Terminal** en Android Studio en la raiz del proyecto.
2. Ejecutar el comando:
   ```powershell
   .\gradlew createDebugCoverageReport