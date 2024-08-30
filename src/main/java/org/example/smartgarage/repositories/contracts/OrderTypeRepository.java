package org.example.smartgarage.repositories.contracts;

import org.example.smartgarage.models.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderTypeRepository extends JpaRepository<ServiceType, Long> {
    ServiceType findByServiceName(String serviceName);
    ServiceType findByServicePrice(BigDecimal servicePrice);
}
