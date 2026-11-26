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
* Asistente Tecnico RAG (Base de Datos Vectorial): Consulta inteligente mediante embeddings y busqueda semantica por vectores contra el servidor en AWS EC2 (`mcmovilcare_rag_api`) para diagnostico y soporte tecnico.
* Persistencia Local (Room): Almacenamiento en SQLite con sincronizacion reactiva mediante Flow y corrutinas para tablas de Usuarios, Equipos y Ordenes de Trabajo.
* Recursos Nativos: Solicitud de permisos en tiempo de ejecucion, notificaciones locales en la barra de estado, escaneo con camara y generacion nativa de documentos PDF.
* Conexion a Red: Estructura de consumo de API REST configurada mediante Retrofit y Gson.

## Pasos para Ejecutar
1. Abrir Android Studio, luego seleccionar Clone.
2. Copiar el link https://github.com/MemaVE18/MCmovilCare1.0.git del proyecto (puedes copiar directamente este tambien) y colocarlo en en respectivo apartado de Clone y seleccionar la carpeta de destino. 
3. Esperar a que Gradle descargue las dependencias y termine la sincronizacion (Sync Project with Gradle Files).
4. Configurar un emulador o conectar un dispositivo fisico/emulado con Android 8.0 (API 26) o superior.
5. Presionar el boton Run 'app' (Shift + F10) en Android Studio.
6. Para pruebas rapidas en el login sin crear usuarios previos, ingresar:

Admin:
admin@duoc.cl
1234

Técnico:
tecnico@duoc.cl
1234

Técnico:
emacast.tecnico@duoc.cl
123

Cliente:
cliente@duoc.cl
1234

## Testing
1. Dirigirse a C:\Aplicaciones Mobiles\app\src\androidTest\java\com
2. Luego dar Click derecho sobre el folder "com(androidTest)", buscar Run'Test in 'com"
3. Esperar resultados

## Visualizar cobertura del testing
1. Dirigirse a la terminal de android studio
2. Posicionarse en raiz de proyecto con cd
3. Ejecutar código:
.\gradlew createDebugCoverageReport
4. Buscar linea "View coverage report at file:/C:/Aplicaciones%20Mobiles/app/build/reports/coverage/androidTest/debug/connected/index.html" y dale click

## ######################################################################################################################################################
## Reinicio del Laboratorio AWS Academy (Cambio de IP y Docker)

Este proyecto se conecta a un backend en **AWS EC2** dentro de **AWS Academy Learner Lab**. Cada vez que vence la sesion de 4 horas y vuelvo a iniciar el laboratorio (**Start Lab**), AWS apaga la instancia, detiene el contenedor de Docker y asigna una **nueva direccion IPv4 publica**.

### 1. Iniciar el laboratorio y la instancia EC2

1. Abrir **Canvas / AWS Academy Learner Lab** y presionar **Start Lab**.
2. Esperar a que el indicador junto a **AWS** pase a verde y hacer clic en **AWS** para entrar a la consola.
3. Ir a **EC2 > Instancias** y seleccionar la instancia del proyecto.
4. Si figura como **Detenida (Stopped)**, hacer clic en **Estado de la instancia > Iniciar instancia**.
5. Actualizar la lista y copiar la nueva **Direccion IPv4 publica**.

### 2. Conectar por SSH y levantar el contenedor Docker

1. Abrir **PowerShell** en la raiz del proyecto (`C:\Aplicaciones Mobiles\MCmovilCare1.0`).
2. Ejecutar los siguientes comandos reemplazando `<NUEVA_IP_PUBLICA>` por la IP copiada:

```powershell
\$key = Get-ChildItem -Path "." -Filter *.pem -Recurse | Select-Object -First 1 -ExpandProperty FullName
ssh -i "\$key" ubuntu@<NUEVA_IP_PUBLICA>
```

3. Ya dentro de la terminal de Ubuntu, iniciar el contenedor del backend:

```bash
docker start mcmovilcare_rag_api
```

4. Comprobar que el servidor responde `200 OK`:

```bash
curl -i http://localhost:8080/health
```

### 3. Actualizar la IP en Android Studio

1. Abrir `app/src/main/java/com/example/mcmovilcare/network/RagClient.kt` y reemplazar la IP en `BASE_URL`:

```kotlin
private const val BASE_URL = "http://<NUEVA_IP_PUBLICA>:8080/"
```

2. Abrir `app/src/main/res/xml/network_security_config.xml` y reemplazar la IP debajo del comentario:

```xml
<!-- REEMPLAZAR ESTA IP AL REINICIAR EL LABORATORIO AWS EC2 -->
<domain includeSubdomains="true"><NUEVA_IP_PUBLICA></domain>
```

3. Guardar los cambios y ejecutar la aplicacion (**Run**).


## Clonar y Levantar la Rama de Release (release-branch-1.0) en Otro PC
git clone -b release-branch-1.0 --single-branch https://github.com/MemaVE18/MCmovilCare1.0.git
cd MCmovilCare1.0

## Si ya tienes el repo, y solo quieres clonar otra Branch (reléase versión para ser claro)
git fetch origin
git checkout -b release-branch-1.0 origin/release-branch-1.0