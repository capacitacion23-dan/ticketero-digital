package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisorService {

    private final AdvisorRepository advisorRepository;

    public List<AdvisorResponse> findAll() {
        return advisorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<AdvisorResponse> findById(Long id) {
        return advisorRepository.findById(id).map(this::toResponse);
    }

    public List<AdvisorResponse> findByStatus(AdvisorStatus status) {
        return advisorRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AdvisorResponse> findAvailableAdvisors() {
        return advisorRepository.findAvailableAdvisorsOrderByWorkload()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<AdvisorResponse> findLeastBusyAvailableAdvisor() {
        List<Advisor> availableAdvisors = advisorRepository.findAvailableAdvisorsOrderByWorkload();
        return availableAdvisors.isEmpty() ? 
                Optional.empty() : 
                Optional.of(toResponse(availableAdvisors.get(0)));
    }

    @Transactional
    public AdvisorResponse updateStatus(Long advisorId, AdvisorStatus newStatus) {
        log.info("Updating advisor {} status to: {}", advisorId, newStatus);

        Advisor advisor = advisorRepository.findById(advisorId)
                .orElseThrow(() -> new RuntimeException("Advisor not found with ID: " + advisorId));

        advisor.setStatus(newStatus);
        
        return toResponse(advisor);
    }

    @Transactional
    public void incrementAssignedTicketsCount(Long advisorId) {
        Advisor advisor = advisorRepository.findById(advisorId)
                .orElseThrow(() -> new RuntimeException("Advisor not found with ID: " + advisorId));

        advisor.setAssignedTicketsCount(advisor.getAssignedTicketsCount() + 1);
        
        if (advisor.getStatus() == AdvisorStatus.AVAILABLE) {
            advisor.setStatus(AdvisorStatus.BUSY);
        }

        log.debug("Advisor {} now has {} assigned tickets", advisorId, advisor.getAssignedTicketsCount());
    }

    @Transactional
    public void decrementAssignedTicketsCount(Long advisorId) {
        Advisor advisor = advisorRepository.findById(advisorId)
                .orElseThrow(() -> new RuntimeException("Advisor not found with ID: " + advisorId));

        int currentCount = advisor.getAssignedTicketsCount();
        if (currentCount > 0) {
            advisor.setAssignedTicketsCount(currentCount - 1);
            
            if (advisor.getAssignedTicketsCount() == 0 && advisor.getStatus() == AdvisorStatus.BUSY) {
                advisor.setStatus(AdvisorStatus.AVAILABLE);
            }
        }

        log.debug("Advisor {} now has {} assigned tickets", advisorId, advisor.getAssignedTicketsCount());
    }

    private AdvisorResponse toResponse(Advisor advisor) {
        return new AdvisorResponse(
                advisor.getId(),
                advisor.getName(),
                advisor.getEmail(),
                advisor.getStatus(),
                advisor.getModuleNumber(),
                advisor.getAssignedTicketsCount(),
                advisor.getCreatedAt(),
                advisor.getUpdatedAt()
        );
    }
}