package com.example.ticketero.scheduler;

import com.example.ticketero.service.QueueProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que procesa las colas de tickets automáticamente
 * Se ejecuta cada 5 segundos para asignar tickets a asesores disponibles
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorScheduler {

    private final QueueProcessorService queueProcessorService;

    /**
     * Procesa las colas de tickets cada 5 segundos
     * - Asigna tickets en espera a asesores disponibles
     * - Actualiza posiciones en cola
     * - Marca tickets como PROXIMO cuando corresponde
     */
    @Scheduled(fixedRate = 5000) // 5 segundos
    public void processQueues() {
        try {
            log.debug("Queue processor scheduler executing...");
            queueProcessorService.processQueues();
        } catch (Exception e) {
            log.error("Error processing queues in scheduler", e);
        }
    }
}