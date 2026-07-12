package com.duoc.gestionguiasdespacho.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GUIAS_DESPACHO_MQ")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiaDespachoMq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MENSAJE")
    private Long id;

    @Column(
            name = "MESSAGE_ID",
            nullable = false,
            unique = true,
            length = 100
    )
    private String messageId;

    @Column(name = "NUMERO_GUIA", nullable = false, length = 50)
    private String numeroGuia;

    @Column(name = "TRANSPORTISTA", nullable = false, length = 120)
    private String transportista;

    @Column(name = "DESTINATARIO", nullable = false, length = 120)
    private String destinatario;

    @Column(name = "DIRECCION_DESTINO", nullable = false, length = 200)
    private String direccionDestino;

    @Column(name = "FECHA_GENERACION", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(name = "ESTADO", nullable = false, length = 30)
    private String estado;

    @Column(
            name = "ESTADO_PROCESAMIENTO",
            nullable = false,
            length = 30
    )
    private String estadoProcesamiento;

    @Column(name = "FECHA_RECEPCION", nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "FECHA_PROCESAMIENTO")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "MENSAJE_ERROR", length = 1000)
    private String mensajeError;

    @PrePersist
    private void prePersist() {
        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDateTime.now();
        }

        if (
                estadoProcesamiento == null
                || estadoProcesamiento.isBlank()
        ) {
            estadoProcesamiento = "RECIBIDO";
        }
    }
}
