# ComuniApp 📍🎉

## Plataforma de Eventos Comunitarios

ComuniApp es una aplicación móvil desarrollada con Jetpack Compose y Kotlin, cuyo objetivo es permitir a los ciudadanos crear, descubrir y participar en eventos comunitarios cercanos a su ubicación, fomentando la integración social y la participación activa en la comunidad.

---

## Información Académica

* Universidad: Universidad del Quindío
* Programa: Ingeniería de Sistemas y Computación
* Asignatura: Construcción de Aplicaciones Móviles
* Docente: Carlos Andrés Florez V.
* Proyecto: Proyecto Final

---

## Objetivo del Proyecto

Desarrollar una aplicación móvil funcional que permita:

* Crear y gestionar eventos comunitarios.
* Descubrir eventos cercanos según la ubicación.
* Confirmar asistencia a eventos.
* Moderar y verificar eventos publicados.
* Notificar a los usuarios sobre eventos relevantes en su zona.

---

## Temática Asignada

Temática 5: Plataforma de eventos comunitarios

### Categorías de eventos:

* Deportes
* Cultura
* Académico
* Voluntariado
* Social

---

## Roles del Sistema

### Usuario

* Registro y autenticación.
* Visualización del feed de eventos.
* Creación, edición y eliminación de eventos.
* Confirmación de asistencia.
* Comentarios y participación.
* Gestión de su perfil y estadísticas personales.

### Moderador

* Verificación y aprobación de eventos.
* Rechazo de eventos con motivo.
* Marcado de eventos como finalizados.
* Supervisión del contenido publicado.

---

## Tecnologías Utilizadas

* Lenguaje: Kotlin
* UI: Jetpack Compose (Material 3)
* Arquitectura: MVVM
* Navegación: Navigation Compose
* Estado: StateFlow y ViewModel
* Backend (futuro): Firebase Authentication, Firestore, Firebase Cloud Messaging
* Mapas (futuro): Google Maps o Mapbox

---

## Estructura del Proyecto

```text
comuniapp/
├── core/
│   ├── navigation/
│   └── utils/
├── features/
│   ├── home/
│   ├── login/
│   ├── register/
│   ├── password/
│   └── events/ (pendiente)
├── ui/
│   ├── components/
│   └── theme/
```

---

## Estado del Proyecto

### Fase 1 – Diseño

* Mockups de las pantallas principales.
* Definición de la arquitectura base y temática del proyecto.

### Fase 2 – Funcionalidades básicas

* Pantallas de autenticación (login, registro, recuperación de contraseña).
* Validación de formularios.
* Navegación entre pantallas.
* Manejo de datos en memoria.

### Fase 3 – Funcionalidades completas

* Persistencia de datos.
* Uso de mapas y geolocalización.
* Subida y gestión de imágenes.
* Autenticación real con Firebase.
* Integración de funcionalidades con Inteligencia Artificial.

---

## Requisito de Inteligencia Artificial (Planeado)

Clasificación automática de eventos.
El sistema sugerirá automáticamente la categoría del evento basándose en el título y la descripción ingresados por el usuario, utilizando un modelo de lenguaje (LLM).

---

## Instalación y Ejecución

1. Clonar el repositorio desde GitHub.
2. Abrir el proyecto en Android Studio.
3. Sincronizar Gradle.
4. Ejecutar la aplicación en un emulador o dispositivo físico.

---

## Autores

* Hulbert Alejandro Arango Fajardo
* Julian Andres Ladino Moreno
* Mauricio Rios de la Ossa

---

## Notas Finales

Este proyecto se desarrolla con fines académicos y cumple con los lineamientos establecidos por la asignatura Construcción de Aplicaciones Móviles.
