**```markdown
# RedNorte - Doctor Service 🩺

Microservicio diseñado para el uso exclusivo del personal médico de RedNorte, permitiendo consultar pacientes asignados y registrar atenciones médicas.

## 🛠️ Tecnologías Utilizadas
* **Spring Boot 3.2.5** (Web)
* **RestTemplate** (Para comunicación síncrona HTTP)
* **Lombok**

## ⚙️ Características Principales
* **Puerto de ejecución:** `8084`
* **Integración Síncrona:** A diferencia de otros servicios, este se comunica de forma directa con el `waitlist-service` (puerto 8083) mediante `RestTemplate` para obtener la lista de pacientes filtrada por especialidad y marcar a los pacientes como atendidos.

## 🚀 Ejecución
```bash
mvn spring-boot:run