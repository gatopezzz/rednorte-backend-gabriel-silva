# RedNorte - Waitlist Service 📋

Este microservicio es parte de la arquitectura distribuida del sistema **RedNorte**. Está encargado de gestionar el ingreso, estado y atención de los pacientes en la lista de espera médica.

## 🛠️ Tecnologías Utilizadas
* **Java 17/21**
* **Spring Boot 3.2.5** (Web, Data JPA)
* **OpenFeign** (Para comunicación con `auth-service`)
* **MySQL** (Base de datos relacional)
* **Lombok** (Para reducir código boilerplate)

## ⚙️ Configuración y Requisitos Previos

1. Tener instalado **Java JDK 17 o superior**.
2. Tener instalado **Maven**.
3. Tener un servidor **MySQL** corriendo en el puerto `3306`.
4. Crear la base de datos localmente:
   ```sql
   CREATE DATABASE rednorte_waitlist_db;