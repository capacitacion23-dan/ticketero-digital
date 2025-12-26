package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo ticket
 */
public record CreateTicketRequest(
    @NotBlank(message = "National ID is required")
    @Size(min = 7, max = 20, message = "National ID must be between 7-20 characters")
    String nationalId,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String telefono,

    @NotBlank(message = "Branch office is required")
    @Size(max = 100, message = "Branch office must not exceed 100 characters")
    String branchOffice,

    @NotNull(message = "Queue type is required")
    QueueType queueType
) {}