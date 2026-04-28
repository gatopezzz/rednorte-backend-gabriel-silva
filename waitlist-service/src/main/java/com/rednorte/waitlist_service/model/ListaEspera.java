package com.rednorte.waitlist_service.model;

import jakarta.persistence.*; // Usa javax.persistence.* si usas una versión más antigua de Spring Boot

@Entity
@Table(name = "lista_espera")
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ESTA ES LA CLAVE: Tiene que llamarse "email" a secas
    private String email;

    private String estado;

    // Constructor vacío exigido por Spring
    public ListaEspera() {
    }

    // --- GETTERS Y SETTERS (Sin esto, Java no puede leer el JSON de React) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}