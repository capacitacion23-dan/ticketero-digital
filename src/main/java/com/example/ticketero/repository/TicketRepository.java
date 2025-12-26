package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Query derivadas (Spring genera automáticamente el SQL)
    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);
    
    Optional<Ticket> findByNumero(String numero);
    
    List<Ticket> findByStatus(TicketStatus status);
    
    List<Ticket> findByQueueType(QueueType queueType);
    
    List<Ticket> findByNationalId(String nationalId);
    
    List<Ticket> findByStatusIn(List<TicketStatus> statuses);
    
    List<Ticket> findByQueueTypeAndStatusOrderByCreatedAtAsc(QueueType queueType, TicketStatus status);
    
    long countByStatus(TicketStatus status);
    
    long countByQueueType(QueueType queueType);
    
    boolean existsByNumero(String numero);

    // Queries custom con @Query
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.status IN :statuses 
        AND t.queueType = :queueType 
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findActiveTicketsByQueueType(
        @Param("queueType") QueueType queueType,
        @Param("statuses") List<TicketStatus> statuses
    );

    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.createdAt >= :startDate 
        AND t.createdAt < :endDate 
        ORDER BY t.createdAt DESC
        """)
    List<Ticket> findTicketsByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
        SELECT COUNT(*) FROM ticket 
        WHERE DATE(created_at) = CURRENT_DATE
        """, nativeQuery = true)
    long countTicketsToday();

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/60) 
        FROM ticket 
        WHERE status = 'COMPLETADO' 
        AND DATE(created_at) = CURRENT_DATE
        """, nativeQuery = true)
    Double getAverageWaitTimeToday();
}