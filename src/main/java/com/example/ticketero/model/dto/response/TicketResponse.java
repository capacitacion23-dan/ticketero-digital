package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para tickets
 */
public record TicketResponse(
    Long id,
    UUID codigoReferencia,
    String numero,
    String nationalId,
    String telefono,
    String branchOffice,
    QueueType queueType,
    TicketStatus status,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    Long assignedAdvisorId,
    String assignedAdvisorName,
    Integer assignedModuleNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}