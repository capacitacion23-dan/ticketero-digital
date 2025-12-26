package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.request.UpdateTicketStatusRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.info("Creating ticket for nationalId: {}, queueType: {}", request.nationalId(), request.queueType());

        // Generar número de ticket
        String numero = generateTicketNumber(request.queueType());
        
        // Calcular posición en cola y tiempo estimado
        int positionInQueue = calculateQueuePosition(request.queueType());
        int estimatedWaitMinutes = calculateEstimatedWaitTime(request.queueType(), positionInQueue);

        Ticket ticket = Ticket.builder()
                .nationalId(request.nationalId())
                .telefono(request.telefono())
                .branchOffice(request.branchOffice())
                .queueType(request.queueType())
                .numero(numero)
                .positionInQueue(positionInQueue)
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .status(TicketStatus.EN_ESPERA)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created with number: {} and ID: {}", numero, savedTicket.getId());

        return toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request) {
        log.info("Updating ticket {} to status: {}", ticketId, request.status());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + ticketId));

        ticket.setStatus(request.status());

        if (request.assignedAdvisorId() != null) {
            Advisor advisor = advisorRepository.findById(request.assignedAdvisorId())
                    .orElseThrow(() -> new RuntimeException("Advisor not found with ID: " + request.assignedAdvisorId()));
            ticket.setAssignedAdvisor(advisor);
            ticket.setAssignedModuleNumber(advisor.getModuleNumber());
        }

        if (request.assignedModuleNumber() != null) {
            ticket.setAssignedModuleNumber(request.assignedModuleNumber());
        }

        return toResponse(ticket);
    }

    public Optional<TicketResponse> findById(Long id) {
        return ticketRepository.findById(id).map(this::toResponse);
    }

    public Optional<TicketResponse> findByCodigoReferencia(UUID codigoReferencia) {
        return ticketRepository.findByCodigoReferencia(codigoReferencia).map(this::toResponse);
    }

    public Optional<TicketResponse> findByNumero(String numero) {
        return ticketRepository.findByNumero(numero).map(this::toResponse);
    }

    public List<TicketResponse> findByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TicketResponse> findActiveTicketsByQueueType(QueueType queueType) {
        List<TicketStatus> activeStatuses = TicketStatus.getActiveStatuses();
        return ticketRepository.findActiveTicketsByQueueType(queueType, activeStatuses)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateTicketNumber(QueueType queueType) {
        char prefix = queueType.getPrefix();
        long count = ticketRepository.countByQueueType(queueType) + 1;
        return String.format("%c%02d", prefix, count);
    }

    private int calculateQueuePosition(QueueType queueType) {
        List<TicketStatus> activeStatuses = TicketStatus.getActiveStatuses();
        return (int) ticketRepository.findActiveTicketsByQueueType(queueType, activeStatuses).size() + 1;
    }

    private int calculateEstimatedWaitTime(QueueType queueType, int position) {
        return (position - 1) * queueType.getAvgTimeMinutes();
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCodigoReferencia(),
                ticket.getNumero(),
                ticket.getNationalId(),
                ticket.getTelefono(),
                ticket.getBranchOffice(),
                ticket.getQueueType(),
                ticket.getStatus(),
                ticket.getPositionInQueue(),
                ticket.getEstimatedWaitMinutes(),
                ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getId() : null,
                ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : null,
                ticket.getAssignedModuleNumber(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}