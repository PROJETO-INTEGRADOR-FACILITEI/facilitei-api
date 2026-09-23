package psg.facilitei.Services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Exceptions.BusinessRuleException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService implements StorageService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/avif", "avif");

    private final S3Client s3;
    private final String bucket;
    private final String region;
    private final String publicBaseUrl;

    public S3StorageService(S3Client s3,
                            @Value("${aws.s3.bucket:}") String bucket,
                            @Value("${aws.s3.region}") String region,
                            @Value("${aws.s3.public-base-url:}") String publicBaseUrl) {
        this.s3 = s3;
        this.bucket = bucket == null ? "" : bucket.trim();
        this.region = region;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public StoredObject uploadImage(MultipartFile file, String folder) {
        String contentType = validateImage(file);
        requireConfiguration();
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^/+|/+$", "");
        String key = safeFolder + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredObject(publicUrl(key), key);
        } catch (SdkException | java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível armazenar a imagem no S3.", ex);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        requireConfiguration();
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível remover a imagem do S3.", ex);
        }
    }

    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("A imagem enviada está vazia.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !EXTENSIONS.containsKey(contentType.toLowerCase())) {
            throw new BusinessRuleException("Envie uma imagem JPEG, PNG, WebP, GIF ou AVIF.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessRuleException("A imagem deve ter no máximo 10 MB.");
        }
        return contentType.toLowerCase();
    }

    private void requireConfiguration() {
        if (bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Armazenamento S3 ainda não configurado.");
        }
    }

    private String publicUrl(String key) {
        String encodedKey = String.join("/", java.util.Arrays.stream(key.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .toList());
        if (!publicBaseUrl.isBlank()) return publicBaseUrl + "/" + encodedKey;
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey;
    }
}
