package psg.facilitei.Services;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredObject uploadImage(MultipartFile file, String folder);
    void delete(String key);

    record StoredObject(String url, String key) {}
}
