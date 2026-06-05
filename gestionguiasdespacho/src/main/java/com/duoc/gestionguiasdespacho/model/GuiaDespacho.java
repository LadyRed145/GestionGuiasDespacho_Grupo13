package com.duoc.gestionguiasdespacho.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "GUIAS_DESPACHO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_GUIA")
    private Long id;

    @Column(name = "NUMERO_GUIA", nullable = false, unique = true, length = 50)
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

    @Column(name = "NOMBRE_ARCHIVO", length = 150)
    private String nombreArchivo;

    @Column(name = "RUTA_EFS", length = 300)
    private String rutaEfs;

    @Column(name = "S3_KEY", length = 500)
    private String s3Key;
}