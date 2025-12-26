package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para actualizar el estado de un ticket
 */
public record UpdateTicketStatusRequest(
    @NotNull(message = "Status is required")
    TicketStatus status,

    Long assignedAdvisorId,
    
    Integer assignedModuleNumber
) {}