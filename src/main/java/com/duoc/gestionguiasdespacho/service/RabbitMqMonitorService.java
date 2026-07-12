package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.EstadoColasResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class RabbitMqMonitorService {

    private final RabbitAdmin rabbitAdmin;

    @Value("${app.rabbitmq.guias.queue}")
    private String guiasQueue;

    @Value("${app.rabbitmq.errores.queue}")
    private String erroresQueue;

    public EstadoColasResponse obtenerEstado() {
        int mensajesPrincipal =
                obtenerCantidadMensajes(
                        guiasQueue
                );

        int mensajesErrores =
                obtenerCantidadMensajes(
                        erroresQueue
                );

        return EstadoColasResponse.builder()
                .colaPrincipal(guiasQueue)
                .mensajesColaPrincipal(
                        mensajesPrincipal
                )
                .colaErrores(erroresQueue)
                .mensajesColaErrores(
                        mensajesErrores
                )
                .estado(
                        determinarEstado(
                                mensajesPrincipal,
                                mensajesErrores
                        )
                )
                .fechaConsulta(
                        LocalDateTime.now()
                )
                .build();
    }

    private int obtenerCantidadMensajes(
            String nombreCola
    ) {
        return Optional.ofNullable(
                        rabbitAdmin.getQueueProperties(
                                nombreCola
                        )
                )
                .map(this::extraerCantidadMensajes)
                .orElse(0);
    }

    private int extraerCantidadMensajes(
            Properties propiedades
    ) {
        Object cantidad =
                propiedades.get(
                        RabbitAdmin.QUEUE_MESSAGE_COUNT
                );

        if (cantidad instanceof Number numero) {
            return numero.intValue();
        }

        if (cantidad instanceof String valor) {
            try {
                return Integer.parseInt(
                        valor.trim()
                );
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        return 0;
    }

    private String determinarEstado(
            int mensajesPrincipal,
            int mensajesErrores
    ) {
        if (mensajesErrores > 0) {
            return "CON_ERRORES";
        }

        if (mensajesPrincipal > 0) {
            return "CON_MENSAJES";
        }

        return "DISPONIBLE";
    }
}