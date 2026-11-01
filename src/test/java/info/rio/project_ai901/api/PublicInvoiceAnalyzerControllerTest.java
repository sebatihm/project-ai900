package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.InvoiceAnalysisResponse;
import info.rio.project_ai901.api.dto.InvoiceItemResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.services.invoice.InvoiceIngestionService;
import info.rio.project_ai901.services.invoice.dto.SuspicionReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicInvoiceAnalyzerController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PublicInvoiceAnalyzerControllerTest {

    private static final String PUBLIC_KEY = "test-public-upload-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceIngestionService invoiceIngestionService;

    private UUID supplierId;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
        when(invoiceIngestionService.analyze(any(), any(), eq(supplierId))).thenReturn(analysis(supplierId));
    }

    @Test
    @WithAnonymousUser
    void devuelve401SinHeader() throws Exception {
        mockMvc.perform(multipart("/public/invoice-analyzer/{supplierId}", supplierId)
                        .file(file()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void devuelve401ConHeaderIncorrecto() throws Exception {
        mockMvc.perform(multipart("/public/invoice-analyzer/{supplierId}", supplierId)
                        .file(file())
                        .header("X-Public-Upload-Key", "clave-incorrecta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void devuelve201ConHeaderCorrectoYSinCamposInternos() throws Exception {
        mockMvc.perform(multipart("/public/invoice-analyzer/{supplierId}", supplierId)
                        .file(file())
                        .header("X-Public-Upload-Key", PUBLIC_KEY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.invoiceId").exists())
                .andExpect(jsonPath("$.data.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.data.container").doesNotExist())
                .andExpect(jsonPath("$.data.blobPath").doesNotExist())
                .andExpect(jsonPath("$.data.sha256").doesNotExist())
                .andExpect(jsonPath("$.data.readUrl").doesNotExist())
                .andExpect(jsonPath("$.data.originalFileName").doesNotExist())
                .andExpect(jsonPath("$.data.minConfidence").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permiteJwtPermitidoSinHeader() throws Exception {
        mockMvc.perform(multipart("/public/invoice-analyzer/{supplierId}", supplierId)
                        .file(file()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_HEAD")
    void rechazaJwtSinRolPermitidoSinHeader() throws Exception {
        mockMvc.perform(multipart("/public/invoice-analyzer/{supplierId}", supplierId)
                        .file(file()))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "factura.pdf", "application/pdf", new byte[] { 1, 2, 3 });
    }

    private InvoiceAnalysisResponse analysis(UUID supplierId) {
        return new InvoiceAnalysisResponse(
                UUID.randomUUID(),
                InvoiceStatus.VALID,
                0,
                List.<SuspicionReason>of(),
                supplierId,
                "Proveedor Demo",
                "F-0001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("100.00"),
                new BigDecimal("16.00"),
                new BigDecimal("116.00"),
                "MXN",
                new BigDecimal("0.950"),
                "factura.pdf",
                "hash-sha256",
                "facturas-validas",
                "2026/factura.pdf",
                "/local-storage/facturas-validas/2026/factura.pdf",
                List.of(new InvoiceItemResponse("Producto", BigDecimal.ONE, new BigDecimal("100.00"))),
                LocalDateTime.now());
    }
}
