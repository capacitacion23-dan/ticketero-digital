package com.example.ticketero.testutil;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder para crear datos de prueba consistentes.
 */
public class TestDataBuilder {

    // ============================================================
    // TICKETS
    // ============================================================
    
    public static Ticket.TicketBuilder ticketWaiting() {
        return Ticket.builder()
            .id(1L)
            .codigoReferencia(UUID.randomUUID())
            .numero("C001")
            .nationalId("12345678")
            .telefono("+56912345678")
            .branchOffice("Sucursal Centro")
            .queueType(QueueType.CAJA)
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(1)
            .estimatedWaitMinutes(5)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }
    
    public static Ticket.TicketBuilder ticketInProgress() {
        return ticketWaiting()
            .status(TicketStatus.ATENDIENDO)
            .positionInQueue(0);
    }
    
    public static Ticket.TicketBuilder ticketCompleted() {
        return ticketInProgress()
            .status(TicketStatus.COMPLETADO);
    }

    // ============================================================
    // ADVISORS
    // ============================================================
    
    public static Advisor.AdvisorBuilder advisorAvailable() {
        return Advisor.builder()
            .id(1L)
            .name("María López")
            .email("maria.lopez@banco.com")
            .moduleNumber(1)
            .status(AdvisorStatus.AVAILABLE)
            .assignedTicketsCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }
    
    public static Advisor.AdvisorBuilder advisorBusy() {
        return advisorAvailable()
            .status(AdvisorStatus.BUSY)
            .assignedTicketsCount(1);
    }

    // ============================================================
    // REQUESTS
    // ============================================================
    
    public static CreateTicketRequest validTicketRequest() {
        return new CreateTicketRequest(
            "12345678",
            "+56912345678",
            "Sucursal Centro",
            QueueType.CAJA
        );
    }
    
    public static CreateTicketRequest ticketRequestSinTelefono() {
        return new CreateTicketRequest(
            "12345678",
            null,
            "Sucursal Centro",
            QueueType.CAJA
        );
    }
}