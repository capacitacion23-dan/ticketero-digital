package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService - Unit Tests")
class MessageServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @InjectMocks
    private MessageService messageService;

    // ============================================================
    // SCHEDULE MESSAGE
    // ============================================================
    
    @Nested
    @DisplayName("scheduleMessage()")
    class ScheduleMessage {

        @Test
        @DisplayName("debe crear mensaje con datos correctos")
        void scheduleMessage_debeCrearMensajeConDatosCorrectos() {
            // Given
            Ticket ticket = ticketWaiting().build();
            LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(5);

            // When
            messageService.scheduleMessage(ticket, MessageTemplate.TOTEM_TICKET_CREADO, scheduledTime);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getTicket()).isEqualTo(ticket);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(mensaje.getFechaProgramada()).isEqualTo(scheduledTime);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(Mensaje.EstadoEnvio.PENDIENTE);
            assertThat(mensaje.getIntentos()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("scheduleTicketCreatedMessage()")
    class ScheduleTicketCreatedMessage {

        @Test
        @DisplayName("debe programar mensaje inmediato")
        void scheduleTicketCreated_debeProgramarMensajeInmediato() {
            // Given
            Ticket ticket = ticketWaiting().build();

            // When
            messageService.scheduleTicketCreatedMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(mensaje.getFechaProgramada()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("scheduleProximoTurnoMessage()")
    class ScheduleProximoTurnoMessage {

        @Test
        @DisplayName("debe programar mensaje 5 minutos antes del tiempo estimado")
        void scheduleProximoTurno_debeProgramar5MinutesAntes() {
            // Given
            Ticket ticket = ticketWaiting().estimatedWaitMinutes(10).build();
            LocalDateTime expectedTime = LocalDateTime.now().plusMinutes(5); // 10 - 5

            // When
            messageService.scheduleProximoTurnoMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_PROXIMO_TURNO);
            assertThat(mensaje.getFechaProgramada()).isCloseTo(expectedTime, within(1, java.time.temporal.ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("con tiempo estimado menor a 5 min → debe programar en 1 minuto")
        void scheduleProximoTurno_tiempoMenorA5_debeProgramarEn1Minuto() {
            // Given
            Ticket ticket = ticketWaiting().estimatedWaitMinutes(3).build();

            // When
            messageService.scheduleProximoTurnoMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getFechaProgramada()).isCloseTo(
                LocalDateTime.now().plusMinutes(1), 
                within(1, java.time.temporal.ChronoUnit.MINUTES)
            );
        }
    }

    // ============================================================
    // MESSAGE STATUS MANAGEMENT
    // ============================================================
    
    @Nested
    @DisplayName("markMessageAsSent()")
    class MarkMessageAsSent {

        @Test
        @DisplayName("debe marcar mensaje como enviado con datos correctos")
        void markAsSent_debeMarcaComoEnviadoConDatos() {
            // Given
            Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .estadoEnvio(Mensaje.EstadoEnvio.PENDIENTE)
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            messageService.markMessageAsSent(1L, "telegram123");

            // Then
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(Mensaje.EstadoEnvio.ENVIADO);
            assertThat(mensaje.getTelegramMessageId()).isEqualTo("telegram123");
            assertThat(mensaje.getFechaEnvio()).isNotNull();
        }

        @Test
        @DisplayName("mensaje inexistente → debe lanzar RuntimeException")
        void markAsSent_mensajeInexistente_debeLanzarExcepcion() {
            // Given
            when(mensajeRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> messageService.markMessageAsSent(999L, "telegram123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("markMessageAsFailed()")
    class MarkMessageAsFailed {

        @Test
        @DisplayName("debe marcar como fallido e incrementar intentos")
        void markAsFailed_debeMarcaFallidoEIncrementarIntentos() {
            // Given
            Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .estadoEnvio(Mensaje.EstadoEnvio.PENDIENTE)
                .intentos(2)
                .build();
            
            when(mensajeRepository.findById(1L)).thenReturn(Optional.of(mensaje));

            // When
            messageService.markMessageAsFailed(1L);

            // Then
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(Mensaje.EstadoEnvio.FALLIDO);
            assertThat(mensaje.getIntentos()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findPendingMessages()")
    class FindPendingMessages {

        @Test
        @DisplayName("debe buscar mensajes pendientes listos para enviar")
        void findPending_debeBuscarMensajesPendientes() {
            // Given
            List<Mensaje> mensajes = List.of(
                Mensaje.builder().build(),
                Mensaje.builder().build()
            );
            when(mensajeRepository.findPendingMessagesReadyToSend(any(LocalDateTime.class)))
                .thenReturn(mensajes);

            // When
            List<Mensaje> result = messageService.findPendingMessages();

            // Then
            assertThat(result).hasSize(2);
            verify(mensajeRepository).findPendingMessagesReadyToSend(any(LocalDateTime.class));
        }
    }
}