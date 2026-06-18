package com.rednorte.auth_service.repository;

import com.rednorte.auth_service.model.SolicitudTutoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudTutoriaRepository extends JpaRepository<SolicitudTutoria, Long> {
    
    Optional<SolicitudTutoria> findByIdSolicitanteAndIdPacienteReceptorAndEstado(Long idSolicitante, Long idPacienteReceptor, String estado);
    
    List<SolicitudTutoria> findByIdPacienteReceptorAndEstado(Long idPacienteReceptor, String estado);
    
    List<SolicitudTutoria> findByIdSolicitante(Long idSolicitante);
    
    List<SolicitudTutoria> findByIdPacienteReceptor(Long idPacienteReceptor);
    
}