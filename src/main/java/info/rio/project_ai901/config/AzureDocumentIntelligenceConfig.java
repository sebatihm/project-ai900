package info.rio.project_ai901.config;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.ai.di.provider", havingValue = "azure")
public class AzureDocumentIntelligenceConfig {

    @Bean
    public DocumentIntelligenceClient documentIntelligenceClient(
            @Value("${app.ai.di.endpoint:}") String endpoint,
            @Value("${app.ai.di.key:}") String key) {
        if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "app.ai.di.endpoint/key no estan configurados (AZURE_DOC_INTELLIGENCE_ENDPOINT / AZURE_DOC_INTELLIGENCE_KEY)");
        }
        return new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
    }
}
