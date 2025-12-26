package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.enums.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Query derivadas
    List<Mensaje> findByEstadoEnvio(Mensaje.EstadoEnvio estadoEnvio);
    
    List<Mensaje> findByPlantilla(MessageTemplate plantilla);
    
    List<Mensaje> findByTicketId(Long ticketId);
    
    long countByEstadoEnvio(Mensaje.EstadoEnvio estadoEnvio);

    // Queries custom para el scheduler
    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'PENDIENTE' 
        AND m.fechaProgramada <= :now 
        ORDER BY m.fechaProgramada ASC
        """)
    List<Mensaje> findPendingMessagesReadyToSend(@Param("now") LocalDateTime now);

    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'FALLIDO' 
        AND m.intentos < :maxRetries 
        AND m.fechaProgramada <= :now 
        ORDER BY m.fechaProgramada ASC
        """)
    List<Mensaje> findFailedMessagesForRetry(
        @Param("maxRetries") int maxRetries,
        @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.ticket.id = :ticketId 
        AND m.plantilla = :plantilla
        """)
    List<Mensaje> findByTicketIdAndPlantilla(
        @Param("ticketId") Long ticketId,
        @Param("plantilla") MessageTemplate plantilla
    );
}