package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.model.GuiaDespacho;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoMqRepository;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoMqProcessingService {

    private final GuiaDespachoRepository guiaRepository;
    private final GuiaDespachoMqRepository guiaMqRepository;

    @Transactional
    public GuiaDespachoMq procesarMensaje(
            GuiaDespachoMessage message
    ) {
        validarMensaje(
                message
        );

        String messageId =
                requerirTexto(
                        message.getMessageId(),
                        "El messageId es obligatorio."
                );

        String numeroGuia =
                requerirTexto(
                        message.getNumeroGuia(),
                        "El número de guía es obligatorio."
                );

        String transportista =
                requerirTexto(
                        message.getTransportista(),
                        "El transportista es obligatorio."
                );

        String destinatario =
                requerirTexto(
                        message.getDestinatario(),
                        "El destinatario es obligatorio."
                );

        String direccionDestino =
                requerirTexto(
                        message.getDireccionDestino(),
                        "La dirección de destino es obligatoria."
                );

        Optional<GuiaDespachoMq> mensajeExistente =
                guiaMqRepository.findByMessageId(
                        messageId
                );

        if (mensajeExistente.isPresent()) {
            log.info(
                    "Mensaje RabbitMQ duplicado ignorado. "
                            + "messageId={}, numeroGuia={}",
                    messageId,
                    numeroGuia
            );

            return mensajeExistente.get();
        }

        LocalDate fechaGeneracion =
                message.getFechaGeneracion() != null
                        ? message.getFechaGeneracion()
                        : LocalDate.now();

        String estado =
                normalizarEstado(
                        message.getEstado()
                );

        GuiaDespacho guiaOperativa =
                obtenerOCrearGuiaOperativa(
                        numeroGuia,
                        transportista,
                        destinatario,
                        direccionDestino,
                        fechaGeneracion,
                        estado
                );

        LocalDateTime ahora =
                LocalDateTime.now();

        GuiaDespachoMq trazabilidad =
                GuiaDespachoMq.builder()
                        .messageId(messageId)
                        .numeroGuia(
                                guiaOperativa.getNumeroGuia()
                        )
                        .transportista(
                                guiaOperativa.getTransportista()
                        )
                        .destinatario(
                                guiaOperativa.getDestinatario()
                        )
                        .direccionDestino(
                                guiaOperativa
                                        .getDireccionDestino()
                        )
                        .fechaGeneracion(
                                guiaOperativa
                                        .getFechaGeneracion()
                        )
                        .estado(
                                guiaOperativa.getEstado()
                        )
                        .estadoProcesamiento(
                                "PROCESADO"
                        )
                        .fechaRecepcion(ahora)
                        .fechaProcesamiento(ahora)
                        .mensajeError(null)
                        .build();

        GuiaDespachoMq trazabilidadValida =
                Objects.requireNonNull(
                        trazabilidad,
                        "No fue posible construir "
                                + "la trazabilidad RabbitMQ."
                );

        GuiaDespachoMq guardada =
                Objects.requireNonNull(
                        guiaMqRepository.saveAndFlush(
                                trazabilidadValida
                        ),
                        "No fue posible guardar "
                                + "la trazabilidad RabbitMQ."
                );

        log.info(
                "Mensaje procesado correctamente. "
                        + "messageId={}, numeroGuia={}, "
                        + "guiaOperativaId={}, trazabilidadId={}",
                messageId,
                numeroGuia,
                guiaOperativa.getId(),
                guardada.getId()
        );

        return guardada;
    }

    private GuiaDespacho obtenerOCrearGuiaOperativa(
            String numeroGuia,
            String transportista,
            String destinatario,
            String direccionDestino,
            LocalDate fechaGeneracion,
            String estado
    ) {
        Optional<GuiaDespacho> existente =
                guiaRepository
                        .findByNumeroGuiaIgnoreCase(
                                numeroGuia
                        );

        if (existente.isPresent()) {
            GuiaDespacho guiaExistente =
                    existente.get();

            log.info(
                    "La guía operativa ya existe. "
                            + "Se reutilizará el registro. "
                            + "id={}, numeroGuia={}",
                    guiaExistente.getId(),
                    guiaExistente.getNumeroGuia()
            );

            return guiaExistente;
        }

        GuiaDespacho nuevaGuia =
                GuiaDespacho.builder()
                        .numeroGuia(numeroGuia)
                        .transportista(transportista)
                        .destinatario(destinatario)
                        .direccionDestino(
                                direccionDestino
                        )
                        .fechaGeneracion(
                                fechaGeneracion
                        )
                        .estado(estado)
                        .build();

        GuiaDespacho guiaValida =
                Objects.requireNonNull(
                        nuevaGuia,
                        "No fue posible construir "
                                + "la guía operativa."
                );

        GuiaDespacho guardada =
                Objects.requireNonNull(
                        guiaRepository.saveAndFlush(
                                guiaValida
                        ),
                        "No fue posible guardar "
                                + "la guía operativa."
                );

        log.info(
                "Guía operativa creada desde RabbitMQ. "
                        + "id={}, numeroGuia={}",
                guardada.getId(),
                guardada.getNumeroGuia()
        );

        return guardada;
    }

    private void validarMensaje(
            GuiaDespachoMessage message
    ) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "El mensaje recibido no puede ser nulo."
            );
        }
    }

    private String requerirTexto(
            String valor,
            String mensajeError
    ) {
        if (
                valor == null
                || valor.isBlank()
        ) {
            throw new IllegalArgumentException(
                    mensajeError
            );
        }

        return valor.trim();
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
