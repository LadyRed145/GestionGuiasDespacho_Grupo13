package com.duoc.gestionguiasdespacho.exception;

public class ErrorProcesamientoRabbitMqException
        extends RuntimeException {

    public ErrorProcesamientoRabbitMqException(
            String mensaje
    ) {
        super(mensaje);
    }

    public ErrorProcesamientoRabbitMqException(
            String mensaje,
            Throwable causa
    ) {
        super(
                mensaje,
                causa
        );
    }
}
