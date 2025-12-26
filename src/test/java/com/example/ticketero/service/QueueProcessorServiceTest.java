package com.example.ticketero.service;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueProcessorService - Unit Tests")
class QueueProcessorServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private AdvisorService advisorService;

    @InjectMocks
    private QueueProcessorService queueProcessorService;

    // ============================================================
    // PROCESS QUEUES
    // ============================================================
    
    @Nested
    @DisplayName("processQueues()")
    class ProcessQueues {

        @Test
        @DisplayName("con tickets en espera y advisor disponible → debe asignar ticket")
        void processQueues_conTicketsYAdvisor_debeAsignarTicket() {
            // Given
            Ticket ticketEspera = ticketWaiting().build();
            Advisor advisorDisponible = advisorAvailable().build();
            
            when(ticketRepository.findByStatus(TicketStatus.EN_ESPERA))
                .thenReturn(List.of(ticketEspera));
            when(advisorRepository.findAvailableAdvisorsOrderByWorkload())
                .thenReturn(List.of(advisorDisponible));
            when(ticketRepository.findActiveTicketsByQueueType(any(), any()))
                .thenReturn(List.of());

            // When
            queueProcessorService.processQueues();

            // Then
            assertThat(ticketEspera.getStatus()).isEqualTo(TicketStatus.ATENDIENDO);
            assertThat(ticketEspera.getAssignedAdvisor()).isEqualTo(advisorDisponible);
            assertThat(ticketEspera.getAssignedModuleNumber()).isEqualTo(advisorDisponible.getModuleNumber());
            // Note: positionInQueue is updated in updateTicketToProximoIfApplicable, not set to 0
            
            verify(advisorService).incrementAssignedTicketsCount(advisorDisponible.getId());
        }

        @Test
        @DisplayName("sin advisors disponibles → debe marcar como PROXIMO si está en top 3")
        void processQueues_sinAdvisors_debeMarcaProximoSiEsTop3() {
            // Given
            Ticket ticket1 = ticketWaiting().id(1L).positionInQueue(1).build();
            Ticket ticket2 = ticketWaiting().id(2L).positionInQueue(2).build();
            
            when(ticketRepository.findByStatus(TicketStatus.EN_ESPERA))
                .thenReturn(List.of(ticket1, ticket2));
            when(advisorRepository.findAvailableAdvisorsOrderByWorkload())
                .thenReturn(List.of());
            when(ticketRepository.findActiveTicketsByQueueType(any(), any()))
                .thenReturn(List.of(ticket1, ticket2));

            // When
            queueProcessorService.processQueues();

            // Then
            assertThat(ticket1.getStatus()).isEqualTo(TicketStatus.PROXIMO);
            assertThat(ticket2.getStatus()).isEqualTo(TicketStatus.PROXIMO);
        }

        @Test
        @DisplayName("sin tickets en espera → no debe hacer nada")
        void processQueues_sinTickets_noDebeHacerNada() {
            // Given
            when(ticketRepository.findByStatus(TicketStatus.EN_ESPERA))
                .thenReturn(List.of());

            // When
            queueProcessorService.processQueues();

            // Then
            verify(advisorRepository, never()).findAvailableAdvisorsOrderByWorkload();
            verify(advisorService, never()).incrementAssignedTicketsCount(any());
        }

        @Test
        @DisplayName("debe actualizar posiciones de todos los tickets activos")
        void processQueues_debeActualizarPosiciones() {
            // Given
            Ticket ticketEspera = ticketWaiting().build();
            Ticket ticketProximo = ticketWaiting().status(TicketStatus.PROXIMO).build();
            
            when(ticketRepository.findByStatus(TicketStatus.EN_ESPERA))
                .thenReturn(List.of(ticketEspera));
            when(ticketRepository.findByStatus(TicketStatus.PROXIMO))
                .thenReturn(List.of(ticketProximo));
            when(advisorRepository.findAvailableAdvisorsOrderByWorkload())
                .thenReturn(List.of());
            when(ticketRepository.findActiveTicketsByQueueType(any(), any()))
                .thenReturn(List.of(ticketEspera, ticketProximo));

            // When
            queueProcessorService.processQueues();

            // Then
            verify(ticketRepository, times(2)).findByStatus(TicketStatus.EN_ESPERA);
            verify(ticketRepository).findByStatus(TicketStatus.PROXIMO);
        }
    }

    // ============================================================
    // COMPLETE TICKET
    // ============================================================
    
    @Nested
    @DisplayName("completeTicket()")
    class CompleteTicket {

        @Test
        @DisplayName("con ticket válido → debe completar y liberar advisor")
        void completeTicket_conTicketValido_debeCompletarYLiberarAdvisor() {
            // Given
            Advisor advisor = advisorBusy().build();
            Ticket ticket = ticketInProgress()
                .id(1L)
                .assignedAdvisor(advisor)
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            queueProcessorService.completeTicket(1L);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETADO);
            verify(advisorService).decrementAssignedTicketsCount(advisor.getId());
        }

        @Test
        @DisplayName("ticket sin advisor asignado → debe completar sin liberar advisor")
        void completeTicket_sinAdvisor_debeCompletarSinLiberarAdvisor() {
            // Given
            Ticket ticket = ticketInProgress()
                .id(1L)
                .assignedAdvisor(null)
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            queueProcessorService.completeTicket(1L);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETADO);
            verify(advisorService, never()).decrementAssignedTicketsCount(any());
        }

        @Test
        @DisplayName("ticket inexistente → debe lanzar RuntimeException")
        void completeTicket_ticketInexistente_debeLanzarExcepcion() {
            // Given
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> queueProcessorService.completeTicket(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
        }
    }

    // ============================================================
    // CANCEL TICKET
    // ============================================================
    
    @Nested
    @DisplayName("cancelTicket()")
    class CancelTicket {

        @Test
        @DisplayName("con ticket válido → debe cancelar y liberar advisor")
        void cancelTicket_conTicketValido_debeCancelarYLiberarAdvisor() {
            // Given
            Advisor advisor = advisorBusy().build();
            Ticket ticket = ticketInProgress()
                .id(1L)
                .assignedAdvisor(advisor)
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            queueProcessorService.cancelTicket(1L);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELADO);
            verify(advisorService).decrementAssignedTicketsCount(advisor.getId());
        }

        @Test
        @DisplayName("ticket sin advisor → debe cancelar sin liberar advisor")
        void cancelTicket_sinAdvisor_debeCancelarSinLiberarAdvisor() {
            // Given
            Ticket ticket = ticketWaiting()
                .id(1L)
                .assignedAdvisor(null)
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            queueProcessorService.cancelTicket(1L);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELADO);
            verify(advisorService, never()).decrementAssignedTicketsCount(any());
        }
    }
}