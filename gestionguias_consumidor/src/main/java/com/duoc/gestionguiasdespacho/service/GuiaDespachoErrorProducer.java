package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.exception.ErrorProcesamientoRabbitMqException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoErrorProducer {

    private static final int MAXIMO_DETALLE_ERROR = 1000;

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.errores.exchange}")
    private String erroresExchange;

    @Value("${app.rabbitmq.errores.routing-key}")
    private String erroresRoutingKey;

    public GuiaDespachoMessage enviarError(
            GuiaDespachoMessage mensajeOriginal,
            String detalleError
    ) {
        validarMensaje(
                mensajeOriginal
        );

        GuiaDespachoMessage mensajeError =
                prepararMensajeError(
                        mensajeOriginal,
                        detalleError
                );

        try {
            rabbitTemplate.convertAndSend(
                    erroresExchange,
                    erroresRoutingKey,
                    mensajeError,
                    mensaje -> {
                        mensaje
                                .getMessageProperties()
                                .setMessageId(
                                        mensajeError.getMessageId()
                                );

                        mensaje
                                .getMessageProperties()
                                .setDeliveryMode(
                                        MessageDeliveryMode.PERSISTENT
                                );

                        mensaje
                                .getMessageProperties()
                                .setHeader(
                                        "tipoMensaje",
                                        "GUIA_DESPACHO_ERROR"
                                );

                        mensaje
                                .getMessageProperties()
                                .setHeader(
                                        "numeroGuia",
                                        mensajeError.getNumeroGuia()
                                );

                        return mensaje;
                    }
            );

            log.warn(
                    "Mensaje enviado a la cola de errores. "
                            + "messageId={}, numeroGuia={}, detalle={}",
                    mensajeError.getMessageId(),
                    mensajeError.getNumeroGuia(),
                    mensajeError.getMensajeError()
            );

            return mensajeError;

        } catch (RuntimeException ex) {
            log.error(
                    "No fue posible enviar el mensaje "
                            + "a la cola de errores. "
                            + "messageId={}, numeroGuia={}",
                    mensajeError.getMessageId(),
                    mensajeError.getNumeroGuia(),
                    ex
            );

            throw new ErrorProcesamientoRabbitMqException(
                    "No fue posible enviar la guía "
                            + "a la cola de errores.",
                    ex
            );
        }
    }

    private GuiaDespachoMessage prepararMensajeError(
            GuiaDespachoMessage mensajeOriginal,
            String detalleError
    ) {
        String messageId =
                normalizarMessageId(
                        mensajeOriginal.getMessageId()
                );

        LocalDateTime fechaEnvio =
                mensajeOriginal.getFechaEnvio() != null
                        ? mensajeOriginal.getFechaEnvio()
                        : LocalDateTime.now();

        return mensajeOriginal
                .toBuilder()
                .messageId(
                        messageId
                )
                .fechaEnvio(
                        fechaEnvio
                )
                .mensajeError(
                        normalizarDetalleError(
                                detalleError
                        )
                )
                .build();
    }

    private void validarMensaje(
            GuiaDespachoMessage mensaje
    ) {
        if (mensaje == null) {
            throw new IllegalArgumentException(
                    "El mensaje de la guía no puede ser nulo."
            );
        }

        if (
                mensaje.getNumeroGuia() == null
                || mensaje.getNumeroGuia().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El número de guía del mensaje "
                            + "no puede estar vacío."
            );
        }
    }

    private String normalizarMessageId(
            String messageId
    ) {
        if (
                messageId == null
                || messageId.isBlank()
        ) {
            return UUID
                    .randomUUID()
                    .toString();
        }

        return messageId.trim();
    }

    private String normalizarDetalleError(
            String detalleError
    ) {
        if (
                detalleError == null
                || detalleError.isBlank()
        ) {
            return "Error no especificado durante "
                    + "el procesamiento del mensaje.";
        }

        String detalleNormalizado =
                detalleError.trim();

        if (
                detalleNormalizado.length()
                <= MAXIMO_DETALLE_ERROR
        ) {
            return detalleNormalizado;
        }

        return detalleNormalizado.substring(
                0,
                MAXIMO_DETALLE_ERROR
        );
    }
}
