package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoMqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaDespachoConsultaService {

    private final GuiaDespachoMqRepository guiaMqRepository;

    @Transactional(readOnly = true)
    public List<GuiaDespachoMq> listarProcesados() {
        return guiaMqRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<GuiaDespachoMq> listarPorEstado(
            String estadoProcesamiento
    ) {
        if (
                estadoProcesamiento == null
                || estadoProcesamiento.isBlank()
        ) {
            return listarProcesados();
        }

        String estadoNormalizado =
                estadoProcesamiento
                        .trim()
                        .toUpperCase();

        return guiaMqRepository
                .findByEstadoProcesamientoOrderByFechaRecepcionDesc(
                        estadoNormalizado
                );
    }
}
