package info.rio.project_ai901.api;

import info.rio.project_ai901.services.storage.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageController {

    private final StorageService storageService;

    public LocalStorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/local-storage/{container}/{*path}")
    public ResponseEntity<Resource> get(@PathVariable("container") String container,
            @PathVariable("path") String path) {
        Resource resource = new InputStreamResource(storageService.download(container, path));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName(path)).build().toString())
                .body(resource);
    }

    private String fileName(String path) {
        int slash = path == null ? -1 : path.lastIndexOf('/');
        return slash < 0 ? String.valueOf(path) : path.substring(slash + 1);
    }
}
