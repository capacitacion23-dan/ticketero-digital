package com.example.ticketero.model.dto.response;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para el dashboard administrativo
 */
public record DashboardResponse(
    Integer totalTicketsToday,
    Integer ticketsInQueue,
    Integer ticketsBeingServed,
    Integer ticketsCompleted,
    Double averageWaitTime,
    List<AdvisorResponse> advisors,
    Map<String, Integer> ticketsByQueueType,
    Map<String, Integer> ticketsByStatus
) {}