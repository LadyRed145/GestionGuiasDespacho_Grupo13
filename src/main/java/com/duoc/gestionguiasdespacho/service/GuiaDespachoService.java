package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoRequest;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoResponse;
import com.duoc.gestionguiasdespacho.exception.RecursoNoEncontradoException;
import com.duoc.gestionguiasdespacho.model.GuiaDespacho;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

    private final GuiaDespachoRepository guiaRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.efs.base-path:/mnt/efs/guias}")
    private String efsBasePath;

    public GuiaDespachoResponse crear(GuiaDespachoRequest request) {
        validarRequest(request);

        String numeroGuia = validarTexto(request.getNumeroGuia(), "El número de guía es obligatorio.");
        String transportista = validarTexto(request.getTransportista(), "El transportista es obligatorio.");
        String destinatario = validarTexto(request.getDestinatario(), "El destinatario es obligatorio.");
        String direccionDestino = validarTexto(request.getDireccionDestino(), "La dirección de destino es obligatoria.");

        if (guiaRepository.existsByNumeroGuia(numeroGuia)) {
            throw new IllegalArgumentException("Ya existe una guía con ese número.");
        }

        GuiaDespacho guia = GuiaDespacho.builder()
                .numeroGuia(numeroGuia)
                .transportista(transportista)
                .destinatario(destinatario)
                .direccionDestino(direccionDestino)
                .fechaGeneracion(request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now())
                .estado(normalizarEstado(request.getEstado(), "CREADA"))
                .build();

        return toResponse(guardarGuia(guia));
    }

    public GuiaDespachoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidad(id));
    }

    public GuiaDespachoResponse generarYSubirAS3(Long id) {
        GuiaDespacho guia = buscarEntidad(id);

        String s3KeyAnterior = guia.getS3Key();
        String rutaEfsAnterior = guia.getRutaEfs();

        try {
            Path carpetaEfs = Path.of(efsBasePath);
            Files.createDirectories(carpetaEfs);

            String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
            String nombreArchivo = "guia-" + limpiarNombreCarpeta(numeroGuia) + ".txt";
            Path archivoEfs = carpetaEfs.resolve(nombreArchivo);

            String contenido = construirContenidoGuia(guia);
            Files.writeString(archivoEfs, contenido, StandardCharsets.UTF_8);

            String s3KeyNuevo = construirS3Key(guia, nombreArchivo);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3KeyNuevo)
                    .contentType("text/plain; charset=UTF-8")
                    .contentEncoding("UTF-8")
                    .build();

            s3Client.putObject(
                    putRequest,
                    RequestBody.fromBytes(contenido.getBytes(StandardCharsets.UTF_8))
            );

            borrarS3SiCambio(s3KeyAnterior, s3KeyNuevo);
            borrarEfsSiCambio(rutaEfsAnterior, archivoEfs.toString());

            guia.setNombreArchivo(nombreArchivo);
            guia.setRutaEfs(archivoEfs.toString());
            guia.setS3Key(s3KeyNuevo);
            guia.setEstado("SUBIDA_S3");

            return toResponse(guardarGuia(guia));

        } catch (IOException e) {
            throw new RuntimeException("Error al generar la guía en EFS.", e);
        }
    }

    public ByteArrayResource descargar(Long id) {
        GuiaDespacho guia = buscarEntidad(id);
        validarS3Key(guia);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(guia.getS3Key())
                .build();

        ResponseBytes<GetObjectResponse> archivo = s3Client.getObjectAsBytes(request);
        byte[] contenido = archivo.asByteArray();

        if (contenido == null || contenido.length == 0) {
            throw new RecursoNoEncontradoException("El archivo descargado desde S3 está vacío o no existe.");
        }

        return new ByteArrayResource(contenido);
    }

    public GuiaDespachoResponse actualizar(Long id, GuiaDespachoRequest request) {
        validarRequest(request);

        GuiaDespacho guia = buscarEntidad(id);

        String numeroGuia = validarTexto(request.getNumeroGuia(), "El número de guía es obligatorio.");

        if (guiaRepository.existsByNumeroGuiaAndIdNot(numeroGuia, id)) {
            throw new IllegalArgumentException("Ya existe otra guía con ese número.");
        }

        guia.setNumeroGuia(numeroGuia);
        guia.setTransportista(validarTexto(request.getTransportista(), "El transportista es obligatorio."));
        guia.setDestinatario(validarTexto(request.getDestinatario(), "El destinatario es obligatorio."));
        guia.setDireccionDestino(validarTexto(request.getDireccionDestino(), "La dirección de destino es obligatoria."));
        guia.setFechaGeneracion(request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now());
        guia.setEstado(normalizarEstado(request.getEstado(), "ACTUALIZADA"));

        GuiaDespacho actualizada = guardarGuia(guia);

        if (actualizada.getS3Key() != null && !actualizada.getS3Key().isBlank()) {
            return generarYSubirAS3(actualizada.getId());
        }

        return toResponse(actualizada);
    }

    public void eliminar(Long id) {
        GuiaDespacho guia = buscarEntidad(id);

        borrarS3Seguro(guia.getS3Key());
        borrarEfsSeguro(guia.getRutaEfs());

        guiaRepository.delete(guia);
    }

    public List<GuiaDespachoResponse> listar(String transportista, LocalDate fecha) {
        List<GuiaDespacho> guias;

        if (transportista != null && !transportista.isBlank() && fecha != null) {
            guias = guiaRepository.findByTransportistaIgnoreCaseAndFechaGeneracion(transportista.trim(), fecha);
        } else if (transportista != null && !transportista.isBlank()) {
            guias = guiaRepository.findByTransportistaIgnoreCase(transportista.trim());
        } else if (fecha != null) {
            guias = guiaRepository.findByFechaGeneracion(fecha);
        } else {
            guias = guiaRepository.findAll();
        }

        return guias.stream()
                .map(this::toResponse)
                .toList();
    }

    private GuiaDespacho buscarEntidad(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la guía es obligatorio.");
        }

        return guiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Guía no encontrada con ID: " + id));
    }

    private GuiaDespacho guardarGuia(GuiaDespacho guia) {
        if (guia == null) {
            throw new IllegalArgumentException("La guía no puede ser nula.");
        }

        return guiaRepository.save(guia);
    }

    private String construirContenidoGuia(GuiaDespacho guia) {
        String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
        String transportista = validarTexto(guia.getTransportista(), "La guía debe tener transportista.");
        String destinatario = validarTexto(guia.getDestinatario(), "La guía debe tener destinatario.");
        String direccionDestino = validarTexto(guia.getDireccionDestino(), "La guía debe tener dirección de destino.");
        LocalDate fecha = guia.getFechaGeneracion() != null ? guia.getFechaGeneracion() : LocalDate.now();
        String estado = normalizarEstado(guia.getEstado(), "GENERADA");

        return """
                GUÍA DE DESPACHO
                -----------------
                Número: %s
                Transportista: %s
                Destinatario: %s
                Dirección destino: %s
                Fecha generación: %s
                Estado: %s
                """.formatted(
                numeroGuia,
                transportista,
                destinatario,
                direccionDestino,
                fecha,
                estado
        );
    }

    private String construirS3Key(GuiaDespacho guia, String nombreArchivo) {
        LocalDate fecha = guia.getFechaGeneracion() != null ? guia.getFechaGeneracion() : LocalDate.now();

        String transportista = validarTexto(
                guia.getTransportista(),
                "La guía debe tener transportista para construir la ruta S3."
        );

        return fecha + "/" + limpiarNombreCarpeta(transportista) + "/" + nombreArchivo;
    }

    private String limpiarNombreCarpeta(String texto) {
        return limpiarTextoPlano(texto)
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String limpiarTextoPlano(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("ñ", "n")
                .replace("Ñ", "N")
                .trim();
    }

    private void borrarS3SiCambio(String s3KeyAnterior, String s3KeyNuevo) {
        if (s3KeyAnterior != null
                && !s3KeyAnterior.isBlank()
                && !s3KeyAnterior.equals(s3KeyNuevo)) {
            borrarS3Seguro(s3KeyAnterior);
        }
    }

    private void borrarS3Seguro(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
    }

    private void borrarEfsSiCambio(String rutaAnterior, String rutaNueva) {
        if (rutaAnterior != null
                && !rutaAnterior.isBlank()
                && !rutaAnterior.equals(rutaNueva)) {
            borrarEfsSeguro(rutaAnterior);
        }
    }

    private void borrarEfsSeguro(String rutaEfs) {
        if (rutaEfs == null || rutaEfs.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(rutaEfs));
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar archivo en EFS: " + rutaEfs, e);
        }
    }

    private void validarS3Key(GuiaDespacho guia) {
        if (guia.getS3Key() == null || guia.getS3Key().isBlank()) {
            throw new RecursoNoEncontradoException("La guía aún no tiene archivo asociado en S3.");
        }
    }

    private void validarRequest(GuiaDespachoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula.");
        }
    }

    private String validarTexto(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }

        return valor.trim();
    }

    private String normalizarEstado(String estado, String estadoPorDefecto) {
        if (estado == null || estado.isBlank()) {
            return estadoPorDefecto;
        }

        return estado.trim().toUpperCase();
    }

    private GuiaDespachoResponse toResponse(GuiaDespacho guia) {
        if (guia == null) {
            throw new IllegalArgumentException("La guía no puede ser nula.");
        }

        return GuiaDespachoResponse.builder()
                .id(guia.getId())
                .numeroGuia(guia.getNumeroGuia())
                .transportista(guia.getTransportista())
                .destinatario(guia.getDestinatario())
                .direccionDestino(guia.getDireccionDestino())
                .fechaGeneracion(guia.getFechaGeneracion())
                .estado(guia.getEstado())
                .nombreArchivo(guia.getNombreArchivo())
                .rutaEfs(guia.getRutaEfs())
                .s3Key(guia.getS3Key())
                .build();
    }
}
