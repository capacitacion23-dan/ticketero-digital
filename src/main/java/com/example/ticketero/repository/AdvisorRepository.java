package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {

    // Query derivadas
    List<Advisor> findByStatus(AdvisorStatus status);
    
    Optional<Advisor> findByEmail(String email);
    
    Optional<Advisor> findByModuleNumber(Integer moduleNumber);
    
    List<Advisor> findByStatusOrderByAssignedTicketsCountAsc(AdvisorStatus status);
    
    boolean existsByEmail(String email);
    
    boolean existsByModuleNumber(Integer moduleNumber);
    
    long countByStatus(AdvisorStatus status);

    // Queries custom
    @Query("""
        SELECT a FROM Advisor a 
        WHERE a.status = 'AVAILABLE' 
        ORDER BY a.assignedTicketsCount ASC, a.id ASC
        """)
    List<Advisor> findAvailableAdvisorsOrderByWorkload();

    @Query("""
        SELECT a FROM Advisor a 
        WHERE a.status = 'AVAILABLE' 
        AND a.assignedTicketsCount < :maxTickets 
        ORDER BY a.assignedTicketsCount ASC
        """)
    List<Advisor> findAvailableAdvisorsWithCapacity(@Param("maxTickets") int maxTickets);

    @Query("""
        SELECT a FROM Advisor a 
        WHERE a.assignedTicketsCount > 0 
        ORDER BY a.assignedTicketsCount DESC
        """)
    List<Advisor> findBusyAdvisorsOrderByWorkload();
}