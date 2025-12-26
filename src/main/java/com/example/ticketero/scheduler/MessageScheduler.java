package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler que procesa mensajes programados para Telegram
 * Se ejecuta cada 60 segundos para enviar mensajes pendientes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageScheduler {

    private final MessageService messageService;
    private static final int MAX_RETRIES = 3;

    /**
     * Procesa mensajes pendientes cada 60 segundos
     * - Envía mensajes programados que están listos
     * - Reintenta mensajes fallidos (máximo 3 intentos)
     */
    @Scheduled(fixedRate = 60000) // 60 segundos
    public void processMessages() {
        try {
            log.debug("Message scheduler executing...");
            
            // Procesar mensajes pendientes
            processPendingMessages();
            
            // Reintentar mensajes fallidos
            retryFailedMessages();
            
        } catch (Exception e) {
            log.error("Error processing messages in scheduler", e);
        }
    }

    private void processPendingMessages() {
        List<Mensaje> pendingMessages = messageService.findPendingMessages();
        
        if (!pendingMessages.isEmpty()) {
            log.info("Processing {} pending messages", pendingMessages.size());
            
            for (Mensaje mensaje : pendingMessages) {
                try {
                    sendMessage(mensaje);
                } catch (Exception e) {
                    log.error("Failed to send message {}: {}", mensaje.getId(), e.getMessage());
                    messageService.markMessageAsFailed(mensaje.getId());
                }
            }
        }
    }

    private void retryFailedMessages() {
        List<Mensaje> failedMessages = messageService.findFailedMessagesForRetry(MAX_RETRIES);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            for (Mensaje mensaje : failedMessages) {
                try {
                    sendMessage(mensaje);
                } catch (Exception e) {
                    log.error("Retry failed for message {}: {}", mensaje.getId(), e.getMessage());
                    messageService.markMessageAsFailed(mensaje.getId());
                }
            }
        }
    }

    private void sendMessage(Mensaje mensaje) {
        // Simular envío de mensaje a Telegram
        // En una implementación real, aquí se haría la llamada a la API de Telegram
        
        String messageContent = buildMessageContent(mensaje);
        log.info("Sending Telegram message: {}", messageContent);
        
        // Simular ID de mensaje de Telegram
        String telegramMessageId = "msg_" + System.currentTimeMillis();
        
        // Marcar como enviado
        messageService.markMessageAsSent(mensaje.getId(), telegramMessageId);
        
        log.info("Message {} sent successfully with Telegram ID: {}", 
                mensaje.getId(), telegramMessageId);
    }

    private String buildMessageContent(Mensaje mensaje) {
        var ticket = mensaje.getTicket();
        
        return switch (mensaje.getPlantilla()) {
            case TOTEM_TICKET_CREADO -> String.format(
                "🎫 Ticket creado: %s\n" +
                "Cola: %s\n" +
                "Posición: %d\n" +
                "Tiempo estimado: %d minutos",
                ticket.getNumero(),
                ticket.getQueueType().getDisplayName(),
                ticket.getPositionInQueue(),
                ticket.getEstimatedWaitMinutes()
            );
            
            case TOTEM_PROXIMO_TURNO -> String.format(
                "⏰ ¡Tu turno se acerca!\n" +
                "Ticket: %s\n" +
                "Prepárate, serás atendido pronto.",
                ticket.getNumero()
            );
            
            case TOTEM_ES_TU_TURNO -> String.format(
                "🔔 ¡ES TU TURNO!\n" +
                "Ticket: %s\n" +
                "Dirígete al módulo %d",
                ticket.getNumero(),
                ticket.getAssignedModuleNumber() != null ? ticket.getAssignedModuleNumber() : 0
            );
        };
    }
}