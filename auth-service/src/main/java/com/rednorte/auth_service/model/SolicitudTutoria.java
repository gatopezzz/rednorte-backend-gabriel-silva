package com.rednorte.auth_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitudes_tutoria")
public class SolicitudTutoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @Column(name = "id_paciente_receptor", nullable = false)
    private Long idPacienteReceptor;

    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud = LocalDateTime.now();
}