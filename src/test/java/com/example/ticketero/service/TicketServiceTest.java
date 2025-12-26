package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.CreateTicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
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
import java.util.UUID;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - Unit Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @InjectMocks
    private TicketService ticketService;

    // ============================================================
    // CREAR TICKET
    // ============================================================
    
    @Nested
    @DisplayName("createTicket()")
    class CreateTicket {

        @Test
        @DisplayName("con datos válidos → debe crear ticket y retornar response")
        void createTicket_conDatosValidos_debeCrearTicketYRetornarResponse() {
            // Given
            CreateTicketRequest request = validTicketRequest();
            Ticket ticketGuardado = ticketWaiting()
                .numero("C001")
                .positionInQueue(1)
                .estimatedWaitMinutes(0)
                .build();

            when(ticketRepository.countByQueueType(QueueType.CAJA)).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(eq(QueueType.CAJA), any()))
                .thenReturn(List.of());
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.numero()).isEqualTo("C001");
            assertThat(response.positionInQueue()).isEqualTo(1);
            assertThat(response.estimatedWaitMinutes()).isEqualTo(0);
            assertThat(response.status()).isEqualTo(TicketStatus.EN_ESPERA);
            assertThat(response.queueType()).isEqualTo(QueueType.CAJA);

            verify(ticketRepository).save(any(Ticket.class));
        }

        @Test
        @DisplayName("debe generar número de ticket con prefijo correcto")
        void createTicket_debeGenerarNumeroConPrefijoCorrecto() {
            // Given
            CreateTicketRequest request = validTicketRequest();
            Ticket ticketGuardado = ticketWaiting().numero("C01").build();

            when(ticketRepository.countByQueueType(QueueType.CAJA)).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(any(), any())).thenReturn(List.of());
            when(ticketRepository.save(any())).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response.numero()).startsWith("C");
        }

        @Test
        @DisplayName("para cola PERSONAL_BANKER → debe usar prefijo P")
        void createTicket_colaPersonalBanker_debeUsarPrefijoP() {
            // Given
            CreateTicketRequest request = new CreateTicketRequest(
                "12345678", "+56912345678", "Sucursal Centro", QueueType.PERSONAL_BANKER
            );
            Ticket ticketGuardado = ticketWaiting()
                .queueType(QueueType.PERSONAL_BANKER)
                .numero("P01")
                .build();

            when(ticketRepository.countByQueueType(QueueType.PERSONAL_BANKER)).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(any(), any())).thenReturn(List.of());
            when(ticketRepository.save(any())).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response.numero()).startsWith("P");
        }

        @Test
        @DisplayName("sin teléfono → debe crear ticket igual")
        void createTicket_sinTelefono_debeCrearTicket() {
            // Given
            CreateTicketRequest request = ticketRequestSinTelefono();
            Ticket ticketGuardado = ticketWaiting().telefono(null).build();

            when(ticketRepository.countByQueueType(any())).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(any(), any())).thenReturn(List.of());
            when(ticketRepository.save(any())).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.telefono()).isNull();
        }

        @Test
        @DisplayName("debe calcular posición en cola correctamente")
        void createTicket_debeCalcularPosicionCorrectamente() {
            // Given
            CreateTicketRequest request = validTicketRequest();
            List<Ticket> ticketsActivos = List.of(
                ticketWaiting().build(),
                ticketWaiting().build()
            );
            Ticket ticketGuardado = ticketWaiting().positionInQueue(3).build();

            when(ticketRepository.countByQueueType(any())).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(eq(QueueType.CAJA), any()))
                .thenReturn(ticketsActivos);
            when(ticketRepository.save(any())).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response.positionInQueue()).isEqualTo(3);
        }

        @Test
        @DisplayName("debe calcular tiempo estimado basado en posición")
        void createTicket_debeCalcularTiempoEstimado() {
            // Given
            CreateTicketRequest request = validTicketRequest();
            Ticket ticketGuardado = ticketWaiting()
                .positionInQueue(3)
                .estimatedWaitMinutes(10) // (3-1) * 5 = 10
                .build();

            when(ticketRepository.countByQueueType(any())).thenReturn(0L);
            when(ticketRepository.findActiveTicketsByQueueType(any(), any()))
                .thenReturn(List.of(ticketWaiting().build(), ticketWaiting().build()));
            when(ticketRepository.save(any())).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.createTicket(request);

            // Then
            assertThat(response.estimatedWaitMinutes()).isEqualTo(10);
        }
    }

    // ============================================================
    // OBTENER TICKET
    // ============================================================
    
    @Nested
    @DisplayName("findByCodigoReferencia()")
    class FindByCodigoReferencia {

        @Test
        @DisplayName("con UUID existente → debe retornar ticket")
        void findByCodigoReferencia_conUuidExistente_debeRetornarTicket() {
            // Given
            UUID codigo = UUID.randomUUID();
            Ticket ticket = ticketWaiting()
                .codigoReferencia(codigo)
                .numero("C001")
                .build();

            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.of(ticket));

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().codigoReferencia()).isEqualTo(codigo);
            assertThat(response.get().numero()).isEqualTo("C001");
        }

        @Test
        @DisplayName("con UUID inexistente → debe retornar Optional.empty()")
        void findByCodigoReferencia_conUuidInexistente_debeRetornarEmpty() {
            // Given
            UUID codigo = UUID.randomUUID();
            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.empty());

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isEmpty();
        }
    }
}