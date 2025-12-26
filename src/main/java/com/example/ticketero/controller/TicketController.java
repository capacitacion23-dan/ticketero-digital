package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.request.UpdateTicketStatusRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.service.MessageService;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request
    ) {
        log.info("POST /api/tickets - Creating ticket for nationalId: {}", request.nationalId());
        
        TicketResponse response = ticketService.createTicket(request);
        
        // Programar mensajes automáticos
        var ticket = ticketService.findById(response.id()).orElseThrow();
        // Aquí se programarían los mensajes, pero necesitamos la entity
        // Por simplicidad, lo dejamos comentado por ahora
        
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /api/tickets/{} - Retrieving ticket", id);
        
        return ticketService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigoReferencia}")
    public ResponseEntity<TicketResponse> getTicketByCodigoReferencia(
            @PathVariable UUID codigoReferencia
    ) {
        log.info("GET /api/tickets/codigo/{} - Retrieving ticket by reference code", codigoReferencia);
        
        return ticketService.findByCodigoReferencia(codigoReferencia)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<TicketResponse> getTicketByNumero(@PathVariable String numero) {
        log.info("GET /api/tickets/numero/{} - Retrieving ticket by number", numero);
        
        return ticketService.findByNumero(numero)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) QueueType queueType
    ) {
        log.info("GET /api/tickets - Retrieving tickets with status: {}, queueType: {}", status, queueType);
        
        if (status != null) {
            return ResponseEntity.ok(ticketService.findByStatus(status));
        }
        
        if (queueType != null) {
            return ResponseEntity.ok(ticketService.findActiveTicketsByQueueType(queueType));
        }
        
        // Si no hay filtros, retornar error 400
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        log.info("PUT /api/tickets/{}/status - Updating status to: {}", id, request.status());
        
        TicketResponse response = ticketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queue/{queueType}")
    public ResponseEntity<List<TicketResponse>> getActiveTicketsByQueue(
            @PathVariable QueueType queueType
    ) {
        log.info("GET /api/tickets/queue/{} - Retrieving active tickets", queueType);
        
        List<TicketResponse> tickets = ticketService.findActiveTicketsByQueueType(queueType);
        return ResponseEntity.ok(tickets);
    }
}