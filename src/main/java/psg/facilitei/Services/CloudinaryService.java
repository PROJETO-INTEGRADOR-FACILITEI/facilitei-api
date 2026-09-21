package psg.facilitei.Services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import psg.facilitei.Exceptions.BusinessRuleException;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    public record UploadImagemResult(String url, String publicId) {
    }

    @Autowired
    private Cloudinary cloudinary;

    public String uploadArquivo(MultipartFile file) {
        validarImagem(file);
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "facilitei/uploads",
                    "resource_type", "image"));

            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao fazer upload da imagem", e);
        }
    }

    public UploadImagemResult uploadImagemPortfolio(MultipartFile file) {
        validarImagem(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "facilitei/portfolios",
                            "resource_type", "image"));

            return new UploadImagemResult(
                    (String) uploadResult.get("secure_url"),
                    (String) uploadResult.get("public_id"));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao enviar a imagem do portfolio para o Cloudinary", e);
        }
    }

    public void removerImagem(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao remover a imagem do Cloudinary", e);
        }
    }

    private void validarImagem(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("A imagem enviada está vazia.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessRuleException("O arquivo enviado precisa ser uma imagem.");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessRuleException("A imagem deve ter no máximo 10 MB.");
        }
    }
}
