# RedNorte - Auth Service 🔑

Este microservicio es el núcleo de identidad del sistema **RedNorte**. Está encargado de la gestión integral de usuarios, autenticación, asignación de roles médicos y administración de solicitudes de tutoría para pacientes de la tercera edad.

## 🛠️ Tecnologías Utilizadas
* **Java 17**
* **Spring Boot 3.2.5** (Web, Data JPA, Security)
* **MySQL** (Base de datos relacional)
* **Lombok** (Reducción de código boilerplate)
* **Springdoc OpenAPI / Swagger** (Documentación interactiva de la API)
* **BCrypt Password Encoder** (Cifrado de contraseñas)

## ⚙️ Configuración y Requisitos Previos

1. Tener instalado **Java JDK 17**.
2. Tener instalado **Maven**.
3. Servidor **MySQL** ejecutándose en el puerto `3306`.
4. (Opcional) Servidor **RabbitMQ** en el puerto `5672` si se habilitan colas de mensajería futuras.
5. Crear la base de datos localmente antes de ejecutar:
   ```sql
   CREATE DATABASE rednorte_auth_db;