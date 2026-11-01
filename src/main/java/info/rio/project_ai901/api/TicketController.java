package info.rio.project_ai901.api;

import info.rio.project_ai901.api.dto.ApiResponse;
import info.rio.project_ai901.api.dto.AssignTicketRequest;
import info.rio.project_ai901.api.dto.CreateTicketMessageRequest;
import info.rio.project_ai901.api.dto.CreateTicketRequest;
import info.rio.project_ai901.api.dto.PaginatedResponse;
import info.rio.project_ai901.api.dto.TicketDetailResponse;
import info.rio.project_ai901.api.dto.TicketMessageResponse;
import info.rio.project_ai901.api.dto.TicketSummaryResponse;
import info.rio.project_ai901.api.dto.UpdateTicketRatingRequest;
import info.rio.project_ai901.api.dto.UpdateTicketRequest;
import info.rio.project_ai901.api.dto.UpdateTicketStatusRequest;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.models.enums.TicketTopic;
import info.rio.project_ai901.services.ticket.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<TicketSummaryResponse>>> list(
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "topic", required = false) TicketTopic topic,
            @RequestParam(value = "handledBy", required = false) HandledBy handledBy,
            @RequestParam(value = "assignedToId", required = false) UUID assignedToId,
            @RequestParam(value = "offset", defaultValue = "0") long offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        PaginatedResponse<TicketSummaryResponse> result = ticketService.list(customerId, status, topic, handledBy,
                assignedToId, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de tickets obtenido correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> detail(@PathVariable("id") UUID id) {
        TicketDetailResponse result = ticketService.detail(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Detalle de ticket obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketDetailResponse>> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse result = ticketService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Ticket creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> update(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTicketRequest request) {
        TicketDetailResponse result = ticketService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Ticket actualizado correctamente"));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> assign(@PathVariable("id") UUID id,
            @Valid @RequestBody AssignTicketRequest request) {
        TicketDetailResponse result = ticketService.assign(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Ticket asignado correctamente"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> updateStatus(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        TicketDetailResponse result = ticketService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(result, "Estado del ticket actualizado correctamente"));
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> updateRating(@PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTicketRatingRequest request) {
        TicketDetailResponse result = ticketService.updateRating(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Calificacion registrada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        ticketService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ticket eliminado correctamente"));
    }

    @GetMapping("/{id}/message")
    public ResponseEntity<ApiResponse<List<TicketMessageResponse>>> listMessages(@PathVariable("id") UUID id) {
        List<TicketMessageResponse> result = ticketService.listMessages(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "Listado de mensajes obtenido correctamente"));
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(@PathVariable("id") UUID id,
            @Valid @RequestBody CreateTicketMessageRequest request) {
        TicketMessageResponse result = ticketService.addMessage(id, request);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Mensaje agregado correctamente"));
    }
}
