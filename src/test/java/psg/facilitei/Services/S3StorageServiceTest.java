package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    @Test
    void enviaImagemComMetadadosEDevolveUrlDoCdn() {
        S3Client client = mock(S3Client.class);
        S3StorageService service = new S3StorageService(
                client, "facilitei-prod", "sa-east-1", "https://cdn.facilitei.app/");
        MockMultipartFile image = new MockMultipartFile(
                "file", "foto.png", "image/png", new byte[] {1, 2, 3});

        StorageService.StoredObject result = service.uploadImage(image, "facilitei/portfolios");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertEquals("facilitei-prod", request.getValue().bucket());
        assertEquals("image/png", request.getValue().contentType());
        assertTrue(request.getValue().key().startsWith("facilitei/portfolios/"));
        assertTrue(request.getValue().key().endsWith(".png"));
        assertEquals("https://cdn.facilitei.app/" + result.key(), result.url());
    }

    @Test
    void removePelaChaveERecusaOperacaoSemBucket() {
        S3Client client = mock(S3Client.class);
        S3StorageService service = new S3StorageService(
                client, "facilitei-prod", "sa-east-1", "");

        service.delete("facilitei/portfolios/teste.webp");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertEquals("facilitei/portfolios/teste.webp", request.getValue().key());

        S3StorageService disabled = new S3StorageService(client, "", "sa-east-1", "");
        MockMultipartFile image = new MockMultipartFile(
                "file", "foto.png", "image/png", new byte[] {1});
        assertThrows(ResponseStatusException.class,
                () -> disabled.uploadImage(image, "facilitei/uploads"));
    }
}
