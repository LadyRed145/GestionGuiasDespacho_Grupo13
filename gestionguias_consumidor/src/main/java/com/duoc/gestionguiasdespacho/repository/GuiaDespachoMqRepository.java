package com.duoc.gestionguiasdespacho.repository;

import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface GuiaDespachoMqRepository
        extends JpaRepository<GuiaDespachoMq, Long> {

    @Override
    @NonNull
    <S extends GuiaDespachoMq> S save(
            @NonNull S entity
    );

    Optional<GuiaDespachoMq> findByMessageId(
            String messageId
    );

    boolean existsByMessageId(
            String messageId
    );

    List<GuiaDespachoMq>
    findByEstadoProcesamientoOrderByFechaRecepcionDesc(
            String estadoProcesamiento
    );
}
