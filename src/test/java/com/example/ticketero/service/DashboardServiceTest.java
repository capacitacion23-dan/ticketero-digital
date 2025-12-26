package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - Unit Tests")
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorService advisorService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getDashboardMetrics() → debe retornar métricas completas")
    void getDashboardMetrics_debeRetornarMetricasCompletas() {
        // Given
        when(ticketRepository.countTicketsToday()).thenReturn(100L);
        when(ticketRepository.countByStatus(TicketStatus.EN_ESPERA)).thenReturn(15L);
        when(ticketRepository.countByStatus(TicketStatus.ATENDIENDO)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.COMPLETADO)).thenReturn(80L);
        when(ticketRepository.getAverageWaitTimeToday()).thenReturn(12.5);
        
        List<AdvisorResponse> advisors = List.of(
            new AdvisorResponse(1L, "María", "maria@test.com", null, 1, 2, null, null),
            new AdvisorResponse(2L, "Juan", "juan@test.com", null, 2, 1, null, null)
        );
        when(advisorService.findAll()).thenReturn(advisors);

        // Mock counts by queue type
        when(ticketRepository.countByQueueType(QueueType.CAJA)).thenReturn(50L);
        when(ticketRepository.countByQueueType(QueueType.PERSONAL_BANKER)).thenReturn(30L);
        when(ticketRepository.countByQueueType(QueueType.EMPRESAS)).thenReturn(15L);
        when(ticketRepository.countByQueueType(QueueType.GERENCIA)).thenReturn(5L);

        // Mock counts by status
        when(ticketRepository.countByStatus(TicketStatus.PROXIMO)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.CANCELADO)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.NO_ATENDIDO)).thenReturn(1L);

        // When
        DashboardResponse response = dashboardService.getDashboardMetrics();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalTicketsToday()).isEqualTo(100);
        assertThat(response.ticketsInQueue()).isEqualTo(15);
        assertThat(response.ticketsBeingServed()).isEqualTo(5);
        assertThat(response.ticketsCompleted()).isEqualTo(80);
        assertThat(response.averageWaitTime()).isEqualTo(12.5);
        assertThat(response.advisors()).hasSize(2);
        
        // Verificar tickets por tipo de cola
        assertThat(response.ticketsByQueueType())
            .containsEntry("CAJA", 50)
            .containsEntry("PERSONAL_BANKER", 30)
            .containsEntry("EMPRESAS", 15)
            .containsEntry("GERENCIA", 5);
            
        // Verificar tickets por estado
        assertThat(response.ticketsByStatus())
            .containsEntry("EN_ESPERA", 15)
            .containsEntry("ATENDIENDO", 5)
            .containsEntry("COMPLETADO", 80)
            .containsEntry("PROXIMO", 3)
            .containsEntry("CANCELADO", 2)
            .containsEntry("NO_ATENDIDO", 1);
    }

    @Test
    @DisplayName("getDashboardMetrics() con averageWaitTime null → debe usar 0.0")
    void getDashboardMetrics_averageWaitTimeNull_debeUsar0() {
        // Given
        when(ticketRepository.countTicketsToday()).thenReturn(0L);
        when(ticketRepository.countByStatus(any())).thenReturn(0L);
        when(ticketRepository.getAverageWaitTimeToday()).thenReturn(null);
        when(advisorService.findAll()).thenReturn(List.of());
        when(ticketRepository.countByQueueType(any())).thenReturn(0L);

        // When
        DashboardResponse response = dashboardService.getDashboardMetrics();

        // Then
        assertThat(response.averageWaitTime()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getDashboardMetrics() → debe llamar a todos los repositorios necesarios")
    void getDashboardMetrics_debeLlamarATodosLosRepositorios() {
        // Given
        when(ticketRepository.countTicketsToday()).thenReturn(0L);
        when(ticketRepository.countByStatus(any())).thenReturn(0L);
        when(ticketRepository.getAverageWaitTimeToday()).thenReturn(0.0);
        when(advisorService.findAll()).thenReturn(List.of());
        when(ticketRepository.countByQueueType(any())).thenReturn(0L);

        // When
        dashboardService.getDashboardMetrics();

        // Then
        verify(ticketRepository).countTicketsToday();
        verify(ticketRepository, times(2)).countByStatus(TicketStatus.EN_ESPERA); // Called twice in implementation
        verify(ticketRepository, times(2)).countByStatus(TicketStatus.ATENDIENDO); // Called twice in implementation
        verify(ticketRepository, times(2)).countByStatus(TicketStatus.COMPLETADO); // Called twice in implementation
        verify(ticketRepository).getAverageWaitTimeToday();
        verify(advisorService).findAll();
        
        // Verificar que se consulten todos los tipos de cola
        verify(ticketRepository).countByQueueType(QueueType.CAJA);
        verify(ticketRepository).countByQueueType(QueueType.PERSONAL_BANKER);
        verify(ticketRepository).countByQueueType(QueueType.EMPRESAS);
        verify(ticketRepository).countByQueueType(QueueType.GERENCIA);
        
        // Verificar que se consulten todos los estados (algunos se llaman múltiples veces)
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.EN_ESPERA);
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.PROXIMO);
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.ATENDIENDO);
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.COMPLETADO);
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.CANCELADO);
        verify(ticketRepository, atLeast(1)).countByStatus(TicketStatus.NO_ATENDIDO);
    }
}