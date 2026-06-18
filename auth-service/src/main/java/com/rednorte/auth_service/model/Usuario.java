package com.rednorte.auth_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tutor_id")
    private Long tutorId;
    
    private String rut;
    private String nombre;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    private Especialidad especialidad = Especialidad.NINGUNA;

    
    @Column(name = "edad")
    private Integer edad;

    @Column(name = "mensaje_notificacion", length = 500)
    private String mensajeNotificacion;
}