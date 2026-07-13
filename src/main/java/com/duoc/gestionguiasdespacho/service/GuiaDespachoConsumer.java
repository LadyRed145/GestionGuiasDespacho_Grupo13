package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoMessage;
import com.duoc.gestionguiasdespacho.exception.ErrorProcesamientoRabbitMqException;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoConsumer {

    private static final String ESTADO_ERROR_PRUEBA =
            "ERROR_PRUEBA";

    private static final String TRANSPORTISTA_ERROR_PRUEBA =
            "TRANSPORTISTA_DLQ";

    private final RabbitTemplate rabbitTemplate;

    private final GuiaDespachoMqProcessingService
            guiaProcessingService;

    private final GuiaDespachoProducer guiaProducer;

    private final GuiaDespachoMqErrorService
            guiaErrorService;

    @Value("${app.rabbitmq.guias.queue}")
    private String guiasQueue;

    @Value("${app.rabbitmq.consumer.timeout-ms:2000}")
    private long timeoutMs;

    public Optional<GuiaDespachoMq>
    consumirUnMensaje() {
        Object contenido =
                recibirMensaje();

        if (contenido == null) {
            log.info(
                    "No existen mensajes disponibles "
                            + "en la cola principal."
            );

            return Optional.empty();
        }

        if (!(contenido
                instanceof GuiaDespachoMessage message)) {
            throw new ErrorProcesamientoRabbitMqException(
                    "El mensaje recibido desde RabbitMQ "
                            + "tiene un formato no soportado."
            );
        }

        try {
            validarErrorControlado(
                    message
            );

            GuiaDespachoMq resultado =
                    guiaProcessingService
                            .procesarMensaje(
                                    message
                            );

            return Optional.of(
                    resultado
            );

        } catch (RuntimeException ex) {
            manejarMensajeFallido(
                    message,
                    ex
            );

            throw new ErrorProcesamientoRabbitMqException(
                    obtenerMensajeError(ex),
                    ex
            );
        }
    }

    private Object recibirMensaje() {
        try {
            return rabbitTemplate.receiveAndConvert(
                    guiasQueue,
                    timeoutMs
            );

        } catch (RuntimeException ex) {
            log.error(
                    "No fue posible recibir o convertir "
                            + "el mensaje desde RabbitMQ.",
                    ex
            );

            throw new ErrorProcesamientoRabbitMqException(
                    "No fue posible leer el mensaje "
                            + "desde la cola principal.",
                    ex
            );
        }
    }

    private void validarErrorControlado(
            GuiaDespachoMessage message
    ) {
        String estado =
                normalizarTexto(
                        message.getEstado()
                );

        String transportista =
                normalizarTexto(
                        message.getTransportista()
                );

        boolean esEstadoPrueba =
                ESTADO_ERROR_PRUEBA
                        .equalsIgnoreCase(
                                estado
                        );

        boolean esTransportistaPrueba =
                TRANSPORTISTA_ERROR_PRUEBA
                        .equalsIgnoreCase(
                                transportista
                        );

        if (
                esEstadoPrueba
                && esTransportistaPrueba
        ) {
            throw new IllegalStateException(
                    "Error controlado de prueba para validar "
                            + "el registro en Oracle y el envío "
                            + "a la cola de errores."
            );
        }
    }

    private void manejarMensajeFallido(
            GuiaDespachoMessage message,
            RuntimeException ex
    ) {
        String detalleError =
                obtenerMensajeError(
                        ex
                );

        log.error(
                "Error procesando mensaje RabbitMQ. "
                        + "messageId={}, numeroGuia={}, "
                        + "detalle={}",
                message.getMessageId(),
                message.getNumeroGuia(),
                detalleError,
                ex
        );

        registrarErrorEnOracle(
                message,
                detalleError
        );

        enviarAColaDeErrores(
                message,
                detalleError,
                ex
        );
    }

    private void registrarErrorEnOracle(
            GuiaDespachoMessage message,
            String detalleError
    ) {
        try {
            guiaErrorService.registrarError(
                    message,
                    detalleError
            );

        } catch (RuntimeException registroEx) {
            log.error(
                    "No fue posible registrar el error "
                            + "del mensaje en Oracle. "
                            + "messageId={}",
                    message.getMessageId(),
                    registroEx
            );
        }
    }

    private void enviarAColaDeErrores(
            GuiaDespachoMessage message,
            String detalleError,
            RuntimeException errorOriginal
    ) {
        try {
            guiaProducer.enviarError(
                    message,
                    detalleError
            );

        } catch (RuntimeException envioEx) {
            log.error(
                    "No fue posible enviar el mensaje "
                            + "a la cola de errores. "
                            + "messageId={}",
                    message.getMessageId(),
                    envioEx
            );

            errorOriginal.addSuppressed(
                    envioEx
            );
        }
    }

    private String normalizarTexto(
            String valor
    ) {
        if (valor == null) {
            return "";
        }

        return valor.trim();
    }

    private String obtenerMensajeError(
            RuntimeException ex
    ) {
        String mensaje =
                ex.getMessage();

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
