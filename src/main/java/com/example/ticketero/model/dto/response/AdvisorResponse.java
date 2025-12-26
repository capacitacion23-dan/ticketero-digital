package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.AdvisorStatus;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para asesores
 */
public record AdvisorResponse(
    Long id,
    String name,
    String email,
    AdvisorStatus status,
    Integer moduleNumber,
    Integer assignedTicketsCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}