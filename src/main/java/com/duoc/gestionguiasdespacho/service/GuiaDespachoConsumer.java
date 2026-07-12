package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoMqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final GuiaDespachoMqRepository guiaMqRepository;
    private final GuiaDespachoProducer guiaProducer;

    @Value("${app.rabbitmq.guias.queue}")
    private String guiasQueue;

    @Value("${app.rabbitmq.consumer.timeout-ms:2000}")
    private long timeoutMs;

    @Transactional
    public Optional<GuiaDespachoMq> consumirUnMensaje() {
        Object contenido = rabbitTemplate.receiveAndConvert(
                guiasQueue,
                timeoutMs
        );

        if (contenido == null) {
            log.info(
                    "No existen mensajes disponibles "
                            + "en la cola principal."
            );

            return Optional.empty();
        }

        if (!(contenido instanceof GuiaDespachoMessage message)) {
            throw new IllegalArgumentException(
                    "El mensaje recibido desde RabbitMQ "
                            + "tiene un formato no soportado."
            );
        }

        try {
            String messageId = requerirTexto(
                    message.getMessageId(),
                    "El messageId es obligatorio."
            );

            String numeroGuia = requerirTexto(
                    message.getNumeroGuia(),
                    "El número de guía es obligatorio."
            );

            String transportista = requerirTexto(
                    message.getTransportista(),
                    "El transportista es obligatorio."
            );

            String destinatario = requerirTexto(
                    message.getDestinatario(),
                    "El destinatario es obligatorio."
            );

            String direccionDestino = requerirTexto(
                    message.getDireccionDestino(),
                    "La dirección de destino es obligatoria."
            );

            Optional<GuiaDespachoMq> existente =
                    guiaMqRepository.findByMessageId(
                            messageId
                    );

            if (existente.isPresent()) {
                log.info(
                        "Mensaje duplicado ignorado. "
                                + "messageId={}, numeroGuia={}",
                        messageId,
                        numeroGuia
                );

                return existente;
            }

            LocalDateTime ahora = LocalDateTime.now();

            LocalDate fechaGeneracion =
                    message.getFechaGeneracion() != null
                            ? message.getFechaGeneracion()
                            : LocalDate.now();

            GuiaDespachoMq guiaMq =
                    GuiaDespachoMq.builder()
                            .messageId(messageId)
                            .numeroGuia(numeroGuia)
                            .transportista(transportista)
                            .destinatario(destinatario)
                            .direccionDestino(direccionDestino)
                            .fechaGeneracion(fechaGeneracion)
                            .estado(
                                    normalizarEstado(
                                            message.getEstado()
                                    )
                            )
                            .estadoProcesamiento("PROCESADO")
                            .fechaRecepcion(ahora)
                            .fechaProcesamiento(ahora)
                            .mensajeError(null)
                            .build();

            GuiaDespachoMq entidadValida =
                    Objects.requireNonNull(
                            guiaMq,
                            "No fue posible construir "
                                    + "la entidad GuiaDespachoMq."
                    );

            GuiaDespachoMq guardada =
                    guiaMqRepository.save(
                            entidadValida
                    );

            log.info(
                    "Mensaje consumido y guardado en Oracle. "
                            + "messageId={}, numeroGuia={}",
                    guardada.getMessageId(),
                    guardada.getNumeroGuia()
            );

            return Optional.of(guardada);

        } catch (RuntimeException ex) {
            log.error(
                    "Error procesando mensaje RabbitMQ. "
                            + "messageId={}, numeroGuia={}",
                    message.getMessageId(),
                    message.getNumeroGuia(),
                    ex
            );

            guiaProducer.enviarError(
                    message,
                    obtenerMensajeError(ex)
            );

            throw ex;
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

    private String obtenerMensajeError(
            RuntimeException ex
    ) {
        String mensaje = ex.getMessage();

        if (
                mensaje == null
                || mensaje.isBlank()
        ) {
            return "Error no especificado durante "
                    + "el procesamiento del mensaje.";
        }

        return mensaje.trim();
    }
}
