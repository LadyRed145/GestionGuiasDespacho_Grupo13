package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoMqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoMqErrorService {

    private final GuiaDespachoMqRepository guiaMqRepository;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public GuiaDespachoMq registrarError(
            GuiaDespachoMessage message,
            String detalleError
    ) {
        String messageId =
                limpiarTexto(
                        message.getMessageId(),
                        "MESSAGE-ID-NO-DISPONIBLE"
                );

        String numeroGuia =
                limpiarTexto(
                        message.getNumeroGuia(),
                        "GUIA-NO-DISPONIBLE"
                );

        String transportista =
                limpiarTexto(
                        message.getTransportista(),
                        "TRANSPORTISTA-NO-DISPONIBLE"
                );

        String destinatario =
                limpiarTexto(
                        message.getDestinatario(),
                        "DESTINATARIO-NO-DISPONIBLE"
                );

        String direccionDestino =
                limpiarTexto(
                        message.getDireccionDestino(),
                        "DIRECCION-NO-DISPONIBLE"
                );

        String estado =
                limpiarTexto(
                        message.getEstado(),
                        "ERROR"
                )
                        .toUpperCase();

        LocalDate fechaGeneracion =
                message.getFechaGeneracion() != null
                        ? message.getFechaGeneracion()
                        : LocalDate.now();

        LocalDateTime ahora =
                LocalDateTime.now();

        String mensajeError =
                limitarTexto(
                        detalleError,
                        1000
                );

        GuiaDespachoMq registro =
                guiaMqRepository
                        .findByMessageId(messageId)
                        .orElseGet(
                                () -> GuiaDespachoMq
                                        .builder()
                                        .messageId(messageId)
                                        .numeroGuia(numeroGuia)
                                        .transportista(transportista)
                                        .destinatario(destinatario)
                                        .direccionDestino(
                                                direccionDestino
                                        )
                                        .fechaGeneracion(
                                                fechaGeneracion
                                        )
                                        .fechaRecepcion(ahora)
                                        .build()
                        );

        registro.setNumeroGuia(numeroGuia);
        registro.setTransportista(transportista);
        registro.setDestinatario(destinatario);
        registro.setDireccionDestino(
                direccionDestino
        );
        registro.setFechaGeneracion(
                fechaGeneracion
        );
        registro.setEstado(estado);
        registro.setEstadoProcesamiento(
                "ERROR"
        );
        registro.setFechaProcesamiento(
                ahora
        );
        registro.setMensajeError(
                mensajeError
        );

        GuiaDespachoMq guardado =
                guiaMqRepository.saveAndFlush(
                        registro
                );

        log.warn(
                "Error RabbitMQ registrado en Oracle. "
                        + "messageId={}, numeroGuia={}, detalle={}",
                guardado.getMessageId(),
                guardado.getNumeroGuia(),
                guardado.getMensajeError()
        );

        return guardado;
    }

    private String limpiarTexto(
            String valor,
            String valorPorDefecto
    ) {
        if (
                valor == null
                || valor.isBlank()
        ) {
            return valorPorDefecto;
        }

        return valor.trim();
    }

    private String limitarTexto(
            String texto,
            int maximo
    ) {
        if (
                texto == null
                || texto.isBlank()
        ) {
            return "Error no especificado durante "
                    + "el procesamiento del mensaje.";
        }

        String valor =
                texto.trim();

        return valor.length() <= maximo
                ? valor
                : valor.substring(
                        0,
                        maximo
                );
    }
}
