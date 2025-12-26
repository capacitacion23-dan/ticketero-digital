package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private final MensajeRepository mensajeRepository;

    @Transactional
    public void scheduleMessage(Ticket ticket, MessageTemplate template, LocalDateTime scheduledTime) {
        log.info("Scheduling message {} for ticket {} at {}", template, ticket.getNumero(), scheduledTime);

        Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(template)
                .fechaProgramada(scheduledTime)
                .estadoEnvio(Mensaje.EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

        mensajeRepository.save(mensaje);
    }

    @Transactional
    public void scheduleTicketCreatedMessage(Ticket ticket) {
        // Programar mensaje inmediato de confirmación
        scheduleMessage(ticket, MessageTemplate.TOTEM_TICKET_CREADO, LocalDateTime.now());
    }

    @Transactional
    public void scheduleProximoTurnoMessage(Ticket ticket) {
        // Programar mensaje cuando esté próximo (estimado - 5 minutos)
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(
                Math.max(1, ticket.getEstimatedWaitMinutes() - 5)
        );
        scheduleMessage(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, scheduledTime);
    }

    @Transactional
    public void scheduleEsTuTurnoMessage(Ticket ticket) {
        // Programar mensaje cuando sea su turno (tiempo estimado)
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(ticket.getEstimatedWaitMinutes());
        scheduleMessage(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, scheduledTime);
    }

    public List<Mensaje> findPendingMessages() {
        return mensajeRepository.findPendingMessagesReadyToSend(LocalDateTime.now());
    }

    public List<Mensaje> findFailedMessagesForRetry(int maxRetries) {
        return mensajeRepository.findFailedMessagesForRetry(maxRetries, LocalDateTime.now());
    }

    @Transactional
    public void markMessageAsSent(Long messageId, String telegramMessageId) {
        Mensaje mensaje = mensajeRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));

        mensaje.setEstadoEnvio(Mensaje.EstadoEnvio.ENVIADO);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setTelegramMessageId(telegramMessageId);

        log.info("Message {} marked as sent with Telegram ID: {}", messageId, telegramMessageId);
    }

    @Transactional
    public void markMessageAsFailed(Long messageId) {
        Mensaje mensaje = mensajeRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));

        mensaje.setEstadoEnvio(Mensaje.EstadoEnvio.FALLIDO);
        mensaje.setIntentos(mensaje.getIntentos() + 1);

        log.warn("Message {} marked as failed. Attempt: {}", messageId, mensaje.getIntentos());
    }
}