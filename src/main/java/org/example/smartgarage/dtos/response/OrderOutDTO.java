package org.example.smartgarage.dtos.response;

public record OrderOutDTO(
        String serviceType,

        double price,

        String addedOn,

        String updatedOn
) {
}
