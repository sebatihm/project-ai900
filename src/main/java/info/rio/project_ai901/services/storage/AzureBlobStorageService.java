package info.rio.project_ai901.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "azure")
public class AzureBlobStorageService implements StorageService {

    private final BlobServiceClient blobServiceClient;

    public AzureBlobStorageService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    @Override
    public String upload(String container, String folder, String filename, InputStream data, long size,
            String contentType, Map<String, String> metadata) {
        String path = join(folder, filename);
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        containerClient.createIfNotExists();
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (contentType != null && !contentType.isBlank()) {
            headers.setContentType(contentType);
        }
        containerClient.getBlobClient(path).getBlockBlobClient()
                .uploadWithResponse(data, size, headers, metadata, null, null, null, null, null);
        return path;
    }

    @Override
    public InputStream download(String container, String path) {
        return blobServiceClient.getBlobContainerClient(container).getBlobClient(path).openInputStream();
    }

    @Override
    public void delete(String container, String path) {
        blobServiceClient.getBlobContainerClient(container).getBlobClient(path).delete();
    }

    @Override
    public boolean exists(String container, String path) {
        return blobServiceClient.getBlobContainerClient(container).getBlobClient(path).exists();
    }

    @Override
    public String readUrl(String container, String path, Duration ttl) {
        BlobClient blob = blobServiceClient.getBlobContainerClient(container).getBlobClient(path);
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plus(ttl);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry,
                new BlobSasPermission().setReadPermission(true));
        return blob.getBlobUrl() + "?" + blob.generateSas(values);
    }

    private String join(String folder, String filename) {
        if (folder == null || folder.isBlank()) {
            return filename == null ? "" : filename;
        }
        if (filename == null || filename.isBlank()) {
            return folder;
        }
        return folder + "/" + filename;
    }
}
