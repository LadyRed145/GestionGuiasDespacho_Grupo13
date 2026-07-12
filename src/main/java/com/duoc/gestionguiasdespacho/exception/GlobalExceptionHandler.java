package com.duoc.gestionguiasdespacho.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(
            RecursoNoEncontradoException.class
    )
    public ResponseEntity<Map<String, Object>>
    manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request
    ) {
        return construirRespuesta(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<Map<String, Object>>
    manejarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<Map<String, Object>>
    manejarValidaciones(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errores =
                new LinkedHashMap<>();

        for (
                FieldError error
                : ex.getBindingResult()
                .getFieldErrors()
        ) {
            errores.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        Map<String, Object> respuesta =
                construirCuerpo(
                        HttpStatus.BAD_REQUEST,
                        "Existen datos inválidos "
                                + "en la solicitud.",
                        request.getRequestURI()
                );

        respuesta.put(
                "validaciones",
                errores
        );

        return ResponseEntity
                .badRequest()
                .body(respuesta);
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<Map<String, Object>>
    manejarIntegridadDatos(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Violación de integridad de datos "
                        + "en la ruta {}.",
                request.getRequestURI(),
                ex
        );

        return construirRespuesta(
                HttpStatus.CONFLICT,
                "El registro no pudo guardarse "
                        + "porque ya existe o incumple "
                        + "una restricción.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    manejarErrorGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Error interno no controlado "
                        + "en la ruta {}.",
                request.getRequestURI(),
                ex
        );

        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno "
                        + "al procesar la solicitud.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<Map<String, Object>>
    construirRespuesta(
        HttpStatus status,
        String mensaje,
        String ruta
) {
    Map<String, Object> cuerpo =
            construirCuerpo(
                    status,
                    mensaje,
                    ruta
            );

    return ResponseEntity
            .status(status.value())
            .body(cuerpo);
}

    private Map<String, Object> construirCuerpo(
            HttpStatus status,
            String mensaje,
            String ruta
    ) {
        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put(
                "timestamp",
                LocalDateTime.now()
        );

        respuesta.put(
                "status",
                status.value()
        );

        respuesta.put(
                "error",
                status.getReasonPhrase()
        );

        respuesta.put(
                "mensaje",
                mensaje
        );

        respuesta.put(
                "ruta",
                ruta
        );

        return respuesta;
    }
}