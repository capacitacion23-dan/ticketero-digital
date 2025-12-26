package com.example.ticketero.service;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueueProcessorService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorService advisorService;

    @Transactional
    public void processQueues() {
        log.debug("Processing ticket queues");

        // Buscar tickets en espera
        List<Ticket> waitingTickets = ticketRepository.findByStatus(TicketStatus.EN_ESPERA);
        
        for (Ticket ticket : waitingTickets) {
            processTicket(ticket);
        }

        // Actualizar posiciones en cola
        updateQueuePositions();
    }

    private void processTicket(Ticket ticket) {
        // Buscar asesor disponible con menor carga
        Optional<Advisor> availableAdvisor = advisorRepository.findAvailableAdvisorsOrderByWorkload()
                .stream()
                .findFirst();

        if (availableAdvisor.isPresent()) {
            assignTicketToAdvisor(ticket, availableAdvisor.get());
        } else {
            // Marcar como próximo si está en las primeras 3 posiciones
            updateTicketToProximoIfApplicable(ticket);
        }
    }

    private void assignTicketToAdvisor(Ticket ticket, Advisor advisor) {
        log.info("Assigning ticket {} to advisor {} (module {})", 
                ticket.getNumero(), advisor.getName(), advisor.getModuleNumber());

        ticket.setAssignedAdvisor(advisor);
        ticket.setAssignedModuleNumber(advisor.getModuleNumber());
        ticket.setStatus(TicketStatus.ATENDIENDO);
        ticket.setPositionInQueue(0);

        // Actualizar contador del asesor
        advisorService.incrementAssignedTicketsCount(advisor.getId());
    }

    private void updateTicketToProximoIfApplicable(Ticket ticket) {
        // Contar tickets activos de la misma cola
        List<TicketStatus> activeStatuses = TicketStatus.getActiveStatuses();
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByQueueType(
                ticket.getQueueType(), activeStatuses);

        // Encontrar posición del ticket actual
        int position = findTicketPosition(ticket, activeTickets);
        
        if (position <= 3 && ticket.getStatus() == TicketStatus.EN_ESPERA) {
            ticket.setStatus(TicketStatus.PROXIMO);
            log.info("Ticket {} marked as PROXIMO (position {})", ticket.getNumero(), position);
        }

        ticket.setPositionInQueue(position);
    }

    private int findTicketPosition(Ticket targetTicket, List<Ticket> activeTickets) {
        for (int i = 0; i < activeTickets.size(); i++) {
            if (activeTickets.get(i).getId().equals(targetTicket.getId())) {
                return i + 1;
            }
        }
        return activeTickets.size() + 1;
    }

    private void updateQueuePositions() {
        // Actualizar posiciones para todos los tickets activos
        List<TicketStatus> activeStatuses = List.of(TicketStatus.EN_ESPERA, TicketStatus.PROXIMO);
        
        for (TicketStatus status : activeStatuses) {
            List<Ticket> tickets = ticketRepository.findByStatus(status);
            for (Ticket ticket : tickets) {
                updateTicketToProximoIfApplicable(ticket);
            }
        }
    }

    @Transactional
    public void completeTicket(Long ticketId) {
        log.info("Completing ticket with ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + ticketId));

        ticket.setStatus(TicketStatus.COMPLETADO);

        // Liberar asesor
        if (ticket.getAssignedAdvisor() != null) {
            advisorService.decrementAssignedTicketsCount(ticket.getAssignedAdvisor().getId());
        }
    }

    @Transactional
    public void cancelTicket(Long ticketId) {
        log.info("Cancelling ticket with ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + ticketId));

        ticket.setStatus(TicketStatus.CANCELADO);

        // Liberar asesor si estaba asignado
        if (ticket.getAssignedAdvisor() != null) {
            advisorService.decrementAssignedTicketsCount(ticket.getAssignedAdvisor().getId());
        }
    }
}