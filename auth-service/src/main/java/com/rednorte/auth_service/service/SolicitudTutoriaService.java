package com.rednorte.auth_service.service;

import com.rednorte.auth_service.model.SolicitudTutoria;
import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.repository.SolicitudTutoriaRepository;
import com.rednorte.auth_service.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitudTutoriaService {

    @Autowired
    private SolicitudTutoriaRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public SolicitudTutoria crearSolicitud(Long idSolicitante, String emailReceptor) {
        Usuario receptor = usuarioRepository.findByEmail(emailReceptor)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ese correo"));

        Long idPacienteReceptor = receptor.getId();

        if (repository.findByIdSolicitanteAndIdPacienteReceptorAndEstado(idSolicitante, idPacienteReceptor, "PENDIENTE").isEmpty()) {
            SolicitudTutoria sol = new SolicitudTutoria();
            sol.setIdSolicitante(idSolicitante);
            sol.setIdPacienteReceptor(idPacienteReceptor);
            sol.setEstado("PENDIENTE");
            
            // Retornamos el objeto guardado para que el Controller lo pueda enviar en el ResponseEntity
            return repository.save(sol);
        } else {
            throw new RuntimeException("Ya existe una solicitud pendiente con este paciente.");
        }
    }

    public List<SolicitudTutoria> obtenerSolicitudesEnviadas(Long idSolicitante) {
        return repository.findByIdSolicitante(idSolicitante);
    }

    public List<SolicitudTutoria> obtenerSolicitudesPendientes(Long idUsuario) {
        // Combinamos ambas listas para que en el panel de React puedas ver 
        // tanto si enviaste la solicitud como si la recibiste.
        List<SolicitudTutoria> enviadas = repository.findByIdSolicitante(idUsuario);
        List<SolicitudTutoria> recibidas = repository.findByIdPacienteReceptor(idUsuario);
        
        List<SolicitudTutoria> todas = new ArrayList<>();
        if (enviadas != null) todas.addAll(enviadas);
        if (recibidas != null) todas.addAll(recibidas);
        
        return todas;
    }

    public SolicitudTutoria responderSolicitud(Long idSolicitud, String nuevoEstado) {
        SolicitudTutoria sol = repository.findById(idSolicitud)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        sol.setEstado(nuevoEstado);
        return repository.save(sol);
    }

    // --- NUEVA LÓGICA PARA ELIMINAR ---
    public void eliminarSolicitud(Long idSolicitud) {
        if (repository.existsById(idSolicitud)) {
            repository.deleteById(idSolicitud);
        } else {
            throw new RuntimeException("La solicitud de tutoría con ID " + idSolicitud + " no existe.");
        }
    }
}