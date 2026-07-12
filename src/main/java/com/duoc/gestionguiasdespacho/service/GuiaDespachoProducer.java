package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
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
public class GuiaDespachoProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.guias.exchange}")
    private String guiasExchange;

    @Value("${app.rabbitmq.guias.routing-key}")
    private String guiasRoutingKey;

    @Value("${app.rabbitmq.errores.exchange}")
    private String erroresExchange;

    @Value("${app.rabbitmq.errores.routing-key}")
    private String erroresRoutingKey;

    public GuiaDespachoMessage enviarGuia(
            GuiaDespachoMessage message
    ) {
        GuiaDespachoMessage mensajePreparado =
                prepararMensaje(message);

        rabbitTemplate.convertAndSend(
                guiasExchange,
                guiasRoutingKey,
                mensajePreparado,
                mensaje -> {
                    mensaje
                            .getMessageProperties()
                            .setMessageId(
                                    mensajePreparado.getMessageId()
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
                                    "GUIA_DESPACHO"
                            );

                    return mensaje;
                }
        );

        log.info(
                "Guía enviada a RabbitMQ. "
                        + "messageId={}, numeroGuia={}",
                mensajePreparado.getMessageId(),
                mensajePreparado.getNumeroGuia()
        );

        return mensajePreparado;
    }

    public void enviarError(
            GuiaDespachoMessage message,
            String detalleError
    ) {
        GuiaDespachoMessage mensajeError =
                prepararMensaje(message)
                        .toBuilder()
                        .mensajeError(
                                limitarTexto(
                                        detalleError,
                                        1000
                                )
                        )
                        .build();

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

                    return mensaje;
                }
        );

        log.warn(
                "Guía enviada a la cola de errores. "
                        + "messageId={}, numeroGuia={}, error={}",
                mensajeError.getMessageId(),
                mensajeError.getNumeroGuia(),
                mensajeError.getMensajeError()
        );
    }

    private GuiaDespachoMessage prepararMensaje(
            GuiaDespachoMessage message
    ) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "El mensaje de la guía no puede ser nulo."
            );
        }

        String messageId = message.getMessageId();

        if (
                messageId == null
                || messageId.isBlank()
        ) {
            messageId = UUID.randomUUID().toString();
        }

        LocalDateTime fechaEnvio =
                message.getFechaEnvio() != null
                        ? message.getFechaEnvio()
                        : LocalDateTime.now();

        return message
                .toBuilder()
                .messageId(messageId.trim())
                .fechaEnvio(fechaEnvio)
                .build();
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

        String valor = texto.trim();

        return valor.length() <= maximo
                ? valor
                : valor.substring(0, maximo);
    }
}
