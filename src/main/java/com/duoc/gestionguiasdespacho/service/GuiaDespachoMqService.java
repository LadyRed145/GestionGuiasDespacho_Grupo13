package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoRequest;
import com.duoc.gestionguiasdespacho.dto.PublicacionGuiaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GuiaDespachoMqService {

    private final GuiaDespachoProducer guiaProducer;

    public PublicacionGuiaResponse publicar(
            GuiaDespachoRequest request
    ) {
        LocalDate fechaGeneracion =
                request.getFechaGeneracion() != null
                        ? request.getFechaGeneracion()
                        : LocalDate.now();

        String estado = normalizarEstado(
                request.getEstado()
        );

        GuiaDespachoMessage message =
                GuiaDespachoMessage.builder()
                        .numeroGuia(
                                request
                                        .getNumeroGuia()
                                        .trim()
                        )
                        .transportista(
                                request
                                        .getTransportista()
                                        .trim()
                        )
                        .destinatario(
                                request
                                        .getDestinatario()
                                        .trim()
                        )
                        .direccionDestino(
                                request
                                        .getDireccionDestino()
                                        .trim()
                        )
                        .fechaGeneracion(
                                fechaGeneracion
                        )
                        .estado(
                                estado
                        )
                        .fechaEnvio(
                                LocalDateTime.now()
                        )
                        .build();

        GuiaDespachoMessage enviado =
                guiaProducer.enviarGuia(message);

        return PublicacionGuiaResponse.builder()
                .mensaje(
                        "Guía enviada correctamente "
                                + "a la cola principal."
                )
                .messageId(
                        enviado.getMessageId()
                )
                .numeroGuia(
                        enviado.getNumeroGuia()
                )
                .estado(
                        "EN_COLA"
                )
                .fechaPublicacion(
                        enviado.getFechaEnvio()
                )
                .build();
    }

    private String normalizarEstado(
            String estado
    ) {
        if (
                estado == null
                || estado.isBlank()
        ) {
            return "CREADA";
        }

        return estado
                .trim()
                .toUpperCase();
    }
}