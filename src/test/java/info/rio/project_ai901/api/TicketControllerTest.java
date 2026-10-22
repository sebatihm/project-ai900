package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.TicketDetailResponse;
import info.rio.project_ai901.api.dto.TicketMessageResponse;
import info.rio.project_ai901.api.dto.TicketSummaryResponse;
import info.rio.project_ai901.config.SecurityConfig;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.exceptions.ResourceInUseException;
import info.rio.project_ai901.exceptions.ResourceNotFoundException;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.MessageAuthor;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketSentiment;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.services.ticket.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void listaTicketsConFiltroDeEstadoYHandledBy() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(ticketService.list(eq(customerId), eq(TicketStatus.OPEN), eq(null), eq(HandledBy.AGENT), eq(null),
                eq(0L), eq(20)))
                .thenReturn(new PaginatedResponse<>(List.of(new TicketSummaryResponse(id, "Paquete retrasado",
                        TicketStatus.OPEN, TicketChannel.EMAIL, TicketTopic.DELAY, TicketSentiment.NEGATIVE,
                        HandledBy.AGENT, false, customerId, "Hotel Miramar", null, null, null,
                        LocalDateTime.of(2026, 1, 1, 10, 0))), 1L, 0L, 20));

        mockMvc.perform(get("/ticket?customerId=" + customerId + "&status=OPEN&handledBy=AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject").value("Paquete retrasado"))
                .andExpect(jsonPath("$.data.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.items[0].handledBy").value("AGENT"));

        verify(ticketService).list(customerId, TicketStatus.OPEN, null, HandledBy.AGENT, null, 0L, 20);
    }

    @Test
    void listaTicketsFiltrandoPorUsuarioAsignado() throws Exception {
        UUID assignedToId = UUID.randomUUID();
        when(ticketService.list(eq(null), eq(null), eq(null), eq(null), eq(assignedToId), eq(0L), eq(20)))
                .thenReturn(new PaginatedResponse<>(List.of(), 0L, 0L, 20));

        mockMvc.perform(get("/ticket?assignedToId=" + assignedToId))
                .andExpect(status().isOk());

        verify(ticketService).list(null, null, null, null, assignedToId, 0L, 20);
    }

    @Test
    void devuelveElDetalleDelTicketConConteoDeMensajes() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.detail(id)).thenReturn(detail(id, TicketStatus.IN_PROGRESS, null, 3));

        mockMvc.perform(get("/ticket/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.messageCount").value(3))
                .andExpect(jsonPath("$.data.customerName").value("Hotel Miramar"));
    }

    @Test
    void devuelve404CuandoElTicketNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.detail(id)).thenThrow(new ResourceNotFoundException("Ticket no encontrado: " + id));

        mockMvc.perform(get("/ticket/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void creaTicketCon201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(ticketService.create(any())).thenReturn(detail(id, TicketStatus.OPEN, null, 1));

        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"subject\":\"Paquete retrasado\","
                                + "\"channel\":\"EMAIL\",\"topic\":\"DELAY\",\"sentiment\":\"NEGATIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.handledBy").value("HUMAN"))
                .andExpect(jsonPath("$.data.messageCount").value(1));
    }

    @Test
    void rechazaCreateSinClienteCon400() throws Exception {
        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Paquete retrasado\",\"channel\":\"EMAIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("customerId: El cliente es obligatorio"));

        verify(ticketService, never()).create(any());
    }

    @Test
    void rechazaCreateSinAsuntoCon400() throws Exception {
        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"channel\":\"EMAIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("subject: El asunto es obligatorio"));
    }

    @Test
    void rechazaMensajeInicialSinContenidoCon400() throws Exception {
        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"subject\":\"Asunto\","
                                + "\"channel\":\"EMAIL\",\"initialMessage\":{\"author\":\"CUSTOMER\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]")
                        .value("initialMessage.content: El contenido es obligatorio"));

        verify(ticketService, never()).create(any());
    }

    @Test
    void actualizaElTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.update(eq(id), any()))
                .thenReturn(new TicketDetailResponse(id, "Asunto actualizado", TicketStatus.OPEN,
                        TicketChannel.PORTAL, TicketTopic.BILLING, TicketSentiment.POSITIVE, HandledBy.HUMAN,
                        true, null, UUID.randomUUID(), "Hotel Miramar", null, null, null,
                        LocalDateTime.of(2026, 1, 1, 10, 0), 0));

        mockMvc.perform(put("/ticket/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Asunto actualizado\",\"channel\":\"PORTAL\","
                                + "\"topic\":\"BILLING\",\"sentiment\":\"POSITIVE\",\"requiresHumanReview\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Asunto actualizado"))
                .andExpect(jsonPath("$.data.requiresHumanReview").value(true));

        verify(ticketService).update(eq(id), any());
    }

    @Test
    void actualizaElEstadoDelTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.updateStatus(id, TicketStatus.IN_PROGRESS))
                .thenReturn(detail(id, TicketStatus.IN_PROGRESS, null, 0));

        mockMvc.perform(put("/ticket/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void devuelve400EnTransicionDeEstadoInvalida() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.updateStatus(id, TicketStatus.OPEN))
                .thenThrow(new InvalidRequestException(
                        "Transicion de estado no permitida: RESOLVED -> OPEN"));

        mockMvc.perform(put("/ticket/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"))
                .andExpect(jsonPath("$.detail").value("Transicion de estado no permitida: RESOLVED -> OPEN"));
    }

    @Test
    void rechazaStatusFaltanteCon400() throws Exception {
        mockMvc.perform(put("/ticket/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("status: El estado es obligatorio"));
    }

    @Test
    void registraLaCalificacionDelTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.updateRating(eq(id), any())).thenReturn(detail(id, TicketStatus.RESOLVED, 5, 0));

        mockMvc.perform(put("/ticket/{id}/rating", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void rechazaCalificacionFueraDeRangoCon400() throws Exception {
        mockMvc.perform(put("/ticket/{id}/rating", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("rating: La calificacion debe estar entre 1 y 5"));

        verify(ticketService, never()).updateRating(any(), any());
    }

    @Test
    void eliminaElTicket() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/ticket/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(ticketService).delete(id);
    }

    @Test
    void devuelve409AlEliminarTicketNoResuelto() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceInUseException("Solo se puede eliminar un ticket RESUELTO (estado actual: OPEN)"))
                .when(ticketService).delete(id);

        mockMvc.perform(delete("/ticket/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"));
    }

    @Test
    void listaLosMensajesDelTicket() throws Exception {
        UUID id = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(ticketService.listMessages(id)).thenReturn(List.of(new TicketMessageResponse(messageId, id,
                MessageAuthor.CUSTOMER, "Mi pedido no llego", null, null,
                LocalDateTime.of(2026, 1, 1, 10, 0))));

        mockMvc.perform(get("/ticket/{id}/message", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].author").value("CUSTOMER"))
                .andExpect(jsonPath("$.data[0].content").value("Mi pedido no llego"));
    }

    @Test
    void agregaUnMensajeAlTicketCon201() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.addMessage(eq(id), any())).thenReturn(new TicketMessageResponse(UUID.randomUUID(), id,
                MessageAuthor.EMPLOYEE, "Ya estamos revisando tu caso", null, null,
                LocalDateTime.of(2026, 1, 1, 11, 0)));

        mockMvc.perform(post("/ticket/{id}/message", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"EMPLOYEE\",\"content\":\"Ya estamos revisando tu caso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.author").value("EMPLOYEE"));
    }

    @Test
    void rechazaMensajeSinContenidoCon400() throws Exception {
        mockMvc.perform(post("/ticket/{id}/message", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"CUSTOMER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("content: El contenido es obligatorio"));

        verify(ticketService, never()).addMessage(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SUPPORT")
    void asignaElTicketAUnUsuarioDeSoporte() throws Exception {
        UUID id = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        when(ticketService.assign(eq(id), any())).thenReturn(assignedDetail(id, assignedToId));

        mockMvc.perform(put("/ticket/{id}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":\"" + assignedToId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedToId").value(assignedToId.toString()))
                .andExpect(jsonPath("$.data.assignedToName").value("Soporte Uno"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.handledBy").value("HUMAN"));

        verify(ticketService).assign(eq(id), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SUPPORT")
    void desasignaElTicketConAssignedToIdNulo() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.assign(eq(id), any())).thenReturn(detail(id, TicketStatus.OPEN, null, 0));

        mockMvc.perform(put("/ticket/{id}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedToId").doesNotExist());
    }

    @Test
    void devuelve401AlAsignarSinToken() throws Exception {
        mockMvc.perform(put("/ticket/{id}/assign", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).assign(any(), any());
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_HEAD")
    void devuelve403AlAsignarConRolSinPermiso() throws Exception {
        mockMvc.perform(put("/ticket/{id}/assign", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).assign(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SUPPORT")
    void devuelve404AlAsignarAUsuarioInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.assign(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado: " + UUID.randomUUID()));

        mockMvc.perform(put("/ticket/{id}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SUPPORT")
    void devuelve400AlAsignarAUsuarioInactivo() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.assign(eq(id), any()))
                .thenThrow(new InvalidRequestException("El usuario no esta activo: " + UUID.randomUUID()));

        mockMvc.perform(put("/ticket/{id}/assign", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"));
    }

    private TicketDetailResponse detail(UUID id, TicketStatus status, Integer rating, int messageCount) {
        return new TicketDetailResponse(id, "Paquete retrasado", status, TicketChannel.EMAIL, TicketTopic.DELAY,
                TicketSentiment.NEGATIVE, HandledBy.HUMAN, false, rating, UUID.randomUUID(), "Hotel Miramar",
                null, null, null, LocalDateTime.of(2026, 1, 1, 10, 0), messageCount);
    }

    private TicketDetailResponse assignedDetail(UUID id, UUID assignedToId) {
        return new TicketDetailResponse(id, "Paquete retrasado", TicketStatus.IN_PROGRESS, TicketChannel.EMAIL,
                TicketTopic.DELAY, TicketSentiment.NEGATIVE, HandledBy.HUMAN, false, null, UUID.randomUUID(),
                "Hotel Miramar", null, assignedToId, "Soporte Uno", LocalDateTime.of(2026, 1, 1, 10, 0), 0);
    }
}
