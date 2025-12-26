package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
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
@DisplayName("AdvisorService - Unit Tests")
class AdvisorServiceTest {

    @Mock
    private AdvisorRepository advisorRepository;

    @InjectMocks
    private AdvisorService advisorService;

    // ============================================================
    // FIND METHODS
    // ============================================================
    
    @Nested
    @DisplayName("findLeastBusyAvailableAdvisor()")
    class FindLeastBusyAdvisor {

        @Test
        @DisplayName("con advisors disponibles → debe retornar el menos ocupado")
        void findLeastBusy_conDisponibles_debeRetornarMenosOcupado() {
            // Given
            Advisor advisor = advisorAvailable().assignedTicketsCount(2).build();
            when(advisorRepository.findAvailableAdvisorsOrderByWorkload())
                .thenReturn(List.of(advisor));

            // When
            Optional<AdvisorResponse> response = advisorService.findLeastBusyAvailableAdvisor();

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().assignedTicketsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("sin advisors disponibles → debe retornar Optional.empty()")
        void findLeastBusy_sinDisponibles_debeRetornarEmpty() {
            // Given
            when(advisorRepository.findAvailableAdvisorsOrderByWorkload())
                .thenReturn(List.of());

            // When
            Optional<AdvisorResponse> response = advisorService.findLeastBusyAvailableAdvisor();

            // Then
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("debe retornar advisors con status específico")
        void findByStatus_debeRetornarAdvisorsConStatus() {
            // Given
            List<Advisor> advisors = List.of(
                advisorAvailable().build(),
                advisorAvailable().build()
            );
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(advisors);

            // When
            List<AdvisorResponse> responses = advisorService.findByStatus(AdvisorStatus.AVAILABLE);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses).allMatch(r -> r.status() == AdvisorStatus.AVAILABLE);
        }
    }

    // ============================================================
    // UPDATE STATUS
    // ============================================================
    
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("debe actualizar status correctamente")
        void updateStatus_debeActualizarCorrectamente() {
            // Given
            Advisor advisor = advisorAvailable().build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            AdvisorResponse response = advisorService.updateStatus(1L, AdvisorStatus.BUSY);

            // Then
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
            assertThat(response.status()).isEqualTo(AdvisorStatus.BUSY);
        }

        @Test
        @DisplayName("advisor inexistente → debe lanzar RuntimeException")
        void updateStatus_advisorInexistente_debeLanzarExcepcion() {
            // Given
            when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> advisorService.updateStatus(999L, AdvisorStatus.BUSY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
        }
    }

    // ============================================================
    // TICKET COUNT MANAGEMENT
    // ============================================================
    
    @Nested
    @DisplayName("incrementAssignedTicketsCount()")
    class IncrementTicketsCount {

        @Test
        @DisplayName("advisor AVAILABLE → debe incrementar y cambiar a BUSY")
        void increment_advisorAvailable_debeIncrementarYCambiarABusy() {
            // Given
            Advisor advisor = advisorAvailable().assignedTicketsCount(0).build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.incrementAssignedTicketsCount(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(1);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
        }

        @Test
        @DisplayName("advisor ya BUSY → debe solo incrementar contador")
        void increment_advisorBusy_debeSoloIncrementar() {
            // Given
            Advisor advisor = advisorBusy().assignedTicketsCount(2).build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.incrementAssignedTicketsCount(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(3);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
        }
    }

    @Nested
    @DisplayName("decrementAssignedTicketsCount()")
    class DecrementTicketsCount {

        @Test
        @DisplayName("contador > 0 → debe decrementar")
        void decrement_contadorPositivo_debeDecrementar() {
            // Given
            Advisor advisor = advisorBusy().assignedTicketsCount(2).build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.decrementAssignedTicketsCount(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(1);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
        }

        @Test
        @DisplayName("contador llega a 0 y status BUSY → debe cambiar a AVAILABLE")
        void decrement_contadorACeroYBusy_debeCambiarAAvailable() {
            // Given
            Advisor advisor = advisorBusy().assignedTicketsCount(1).build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.decrementAssignedTicketsCount(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
        }

        @Test
        @DisplayName("contador ya en 0 → no debe cambiar")
        void decrement_contadorEnCero_noDebeCambiar() {
            // Given
            Advisor advisor = advisorAvailable().assignedTicketsCount(0).build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            advisorService.decrementAssignedTicketsCount(1L);

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
        }
    }
}