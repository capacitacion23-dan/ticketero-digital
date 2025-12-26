package com.example.ticketero.controller;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.DashboardService;
import com.example.ticketero.service.QueueProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DashboardService dashboardService;
    private final AdvisorService advisorService;
    private final QueueProcessorService queueProcessorService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("GET /api/admin/dashboard - Retrieving dashboard metrics");
        
        DashboardResponse dashboard = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/advisors")
    public ResponseEntity<List<AdvisorResponse>> getAllAdvisors() {
        log.info("GET /api/admin/advisors - Retrieving all advisors");
        
        List<AdvisorResponse> advisors = advisorService.findAll();
        return ResponseEntity.ok(advisors);
    }

    @GetMapping("/advisors/{id}")
    public ResponseEntity<AdvisorResponse> getAdvisorById(@PathVariable Long id) {
        log.info("GET /api/admin/advisors/{} - Retrieving advisor", id);
        
        return advisorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/advisors/status/{status}")
    public ResponseEntity<List<AdvisorResponse>> getAdvisorsByStatus(
            @PathVariable AdvisorStatus status
    ) {
        log.info("GET /api/admin/advisors/status/{} - Retrieving advisors by status", status);
        
        List<AdvisorResponse> advisors = advisorService.findByStatus(status);
        return ResponseEntity.ok(advisors);
    }

    @GetMapping("/advisors/available")
    public ResponseEntity<List<AdvisorResponse>> getAvailableAdvisors() {
        log.info("GET /api/admin/advisors/available - Retrieving available advisors");
        
        List<AdvisorResponse> advisors = advisorService.findAvailableAdvisors();
        return ResponseEntity.ok(advisors);
    }

    @PutMapping("/advisors/{id}/status")
    public ResponseEntity<AdvisorResponse> updateAdvisorStatus(
            @PathVariable Long id,
            @RequestParam AdvisorStatus status
    ) {
        log.info("PUT /api/admin/advisors/{}/status - Updating status to: {}", id, status);
        
        AdvisorResponse response = advisorService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets/{id}/complete")
    public ResponseEntity<Void> completeTicket(@PathVariable Long id) {
        log.info("POST /api/admin/tickets/{}/complete - Completing ticket", id);
        
        queueProcessorService.completeTicket(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tickets/{id}/cancel")
    public ResponseEntity<Void> cancelTicket(@PathVariable Long id) {
        log.info("POST /api/admin/tickets/{}/cancel - Cancelling ticket", id);
        
        queueProcessorService.cancelTicket(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/queues/process")
    public ResponseEntity<Void> processQueues() {
        log.info("POST /api/admin/queues/process - Manually processing queues");
        
        queueProcessorService.processQueues();
        return ResponseEntity.ok().build();
    }
}