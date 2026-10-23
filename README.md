# MCmovil Care

## Descripcion del Proyecto
Aplicacion movil nativa para Android desarrollada en Kotlin para la empresa MCmovil SpA. Este MVP centraliza la gestion de ordenes de trabajo, diagnosticos tecnicos, autorizacion de presupuestos y garantias de soporte de hardware, operando de forma independiente al sistema interno Gestioo.

El proyecto implementa el patron de arquitectura MVVM (Model-View-ViewModel), combinando vistas en XML y Jetpack Compose, persistencia local con Room (SQLite) y consumo de API REST con Retrofit.

## Integrantes
* Daniel Ignacio Alday Troncoso
* Emanuel Castillo

## Funcionalidades Implementadas
* Autenticacion y Roles: Inicio de sesion con persistencia mediante SharedPreferences y redireccion automatica segun tres perfiles (Cliente, Tecnico, Admin).
* Perfil Cliente: Visualizacion reactiva de equipos en reparacion, revision de detalle de ordenes, aprobacion o rechazo de presupuestos y descarga de orden de servicio en formato PDF.
* Perfil Tecnico: Registro de nuevos equipos mediante lectura de codigos QR con camara (Google ML Kit) y actualizacion en tiempo real de diagnosticos, montos y estados de las ordenes.
* Perfil Administrador: Creacion de nuevos usuarios con asignacion de rol en la base de datos local y supervision global del historial de ordenes.
* Persistencia Local (Room): Almacenamiento en SQLite con sincronizacion reactiva mediante Flow y corrutinas para tablas de Usuarios, Equipos y Ordenes de Trabajo.
* Recursos Nativos: Solicitud de permisos en tiempo de ejecucion, notificaciones locales en la barra de estado, escaneo con camara y generacion nativa de documentos PDF.
* Conexion a Red: Estructura de consumo de API REST configurada mediante Retrofit y Gson.

## Pasos para Ejecutar
1. Abrir Android Studio, luego seleccionar Clone.
2. Copiar el link https://github.com/MemaVE18/MCmovilCare1.0.git del proyecto (puedes copiar directamente este tambien) y colocarlo en en respectivo apartado de Clone y seleccionar la carpeta de destino. 
3. Esperar a que Gradle descargue las dependencias y termine la sincronizacion (Sync Project with Gradle Files).
4. Configurar un emulador o conectar un dispositivo fisico/emulado con Android 8.0 (API 26) o superior.
5. Presionar el boton Run 'app' (Shift + F10) en Android Studio.
6. Para pruebas rapidas en el login sin crear usuarios previos, ingresar cualquier correo que contenga la palabra "cliente", "tecnico" o "admin" para acceder al perfil correspondiente.
