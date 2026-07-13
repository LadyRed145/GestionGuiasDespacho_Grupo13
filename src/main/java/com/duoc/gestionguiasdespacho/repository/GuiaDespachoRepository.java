package com.duoc.gestionguiasdespacho.repository;

import com.duoc.gestionguiasdespacho.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GuiaDespachoRepository
        extends JpaRepository<GuiaDespacho, Long> {

    List<GuiaDespacho>
    findByTransportistaIgnoreCase(
            String transportista
    );

    List<GuiaDespacho>
    findByFechaGeneracion(
            LocalDate fechaGeneracion
    );

    List<GuiaDespacho>
    findByTransportistaIgnoreCaseAndFechaGeneracion(
            String transportista,
            LocalDate fechaGeneracion
    );

    Optional<GuiaDespacho>
    findByNumeroGuiaIgnoreCase(
            String numeroGuia
    );

    boolean existsByNumeroGuia(
            String numeroGuia
    );

    boolean existsByNumeroGuiaAndIdNot(
            String numeroGuia,
            Long id
    );
}
