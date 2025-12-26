package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AdvisorService advisorService;

    public DashboardResponse getDashboardMetrics() {
        log.info("Generating dashboard metrics");

        // Métricas básicas
        int totalTicketsToday = (int) ticketRepository.countTicketsToday();
        int ticketsInQueue = (int) ticketRepository.countByStatus(TicketStatus.EN_ESPERA);
        int ticketsBeingServed = (int) ticketRepository.countByStatus(TicketStatus.ATENDIENDO);
        int ticketsCompleted = (int) ticketRepository.countByStatus(TicketStatus.COMPLETADO);

        // Tiempo promedio de espera
        Double averageWaitTime = ticketRepository.getAverageWaitTimeToday();
        if (averageWaitTime == null) {
            averageWaitTime = 0.0;
        }

        // Lista de asesores
        List<AdvisorResponse> advisors = advisorService.findAll();

        // Tickets por tipo de cola
        Map<String, Integer> ticketsByQueueType = Arrays.stream(QueueType.values())
                .collect(Collectors.toMap(
                        QueueType::name,
                        queueType -> (int) ticketRepository.countByQueueType(queueType)
                ));

        // Tickets por estado
        Map<String, Integer> ticketsByStatus = Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(
                        TicketStatus::name,
                        status -> (int) ticketRepository.countByStatus(status)
                ));

        return new DashboardResponse(
                totalTicketsToday,
                ticketsInQueue,
                ticketsBeingServed,
                ticketsCompleted,
                averageWaitTime,
                advisors,
                ticketsByQueueType,
                ticketsByStatus
        );
    }
}