package org.example.smartgarage.services;

import org.example.smartgarage.dtos.response.VisitOutDto;
import org.example.smartgarage.exceptions.EntityDuplicateException;
import org.example.smartgarage.exceptions.EntityNotFoundException;
import org.example.smartgarage.exceptions.UserMismatchException;
import org.example.smartgarage.models.EventLog;
import org.example.smartgarage.models.UserEntity;
import org.example.smartgarage.models.Visit;
import org.example.smartgarage.models.enums.CurrencyCode;
import org.example.smartgarage.models.enums.Status;
import org.example.smartgarage.repositories.contracts.VisitRepository;
import org.example.smartgarage.services.contracts.*;
import org.example.smartgarage.utils.filtering.VisitFilterOptions;
import org.example.smartgarage.utils.filtering.VisitSpecification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class VisitServiceImpl implements VisitService {
    private final VisitRepository visitRepository;
    private final CurrencyService currencyService;
    private final ReportService reportService;
    private final UserService userService;
    private final HistoryService historyService;

    public VisitServiceImpl(VisitRepository visitRepository, CurrencyService currencyService,
                            ReportService reportService, UserService userService, HistoryService historyService) {
        this.visitRepository = visitRepository;
        this.currencyService = currencyService;
        this.reportService = reportService;
        this.userService = userService;
        this.historyService = historyService;
    }

    @Override
    public List<Visit> findAll(VisitFilterOptions visitFilterOptions) {
        VisitSpecification visitSpecification = new VisitSpecification(visitFilterOptions);

        return visitRepository.findAll(visitSpecification);
    }

    @Override
    public Visit findById(long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));
    }

    @Override
    public List<VisitOutDto> calculateCost(List<VisitOutDto> visitOutDtos, CurrencyCode exchangeCurrency) throws IOException {
        if (exchangeCurrency == null) return visitOutDtos;

        double exchangeRate = currencyService.getConversionRate(exchangeCurrency);

        return visitOutDtos.stream().peek(dto -> {
            dto.setExchangeRate(exchangeRate);
            dto.setTotalCost(dto.getTotalCost().multiply(BigDecimal.valueOf(exchangeRate))
                    .setScale(2, RoundingMode.HALF_UP));
            dto.setCurrency(exchangeCurrency.getDescription());
        }).toList();
    }

    @Override
    public ByteArrayOutputStream createPdf(List<VisitOutDto> visits, UserEntity principal) throws IOException {
        return reportService.createPdf(visits, principal);
    }

    @Override
    public Visit create(Visit visit, Long clerkId) {
        if (!visit.getClient().equals(visit.getVehicle().getOwner())){
            throw new UserMismatchException("The customer is not the owner of said vehicle");
        }

        UserEntity clerk = userService.getById(clerkId);
        visit.setClerk(clerk);
        visit.setEventLogs(new ArrayList<>());
        visit.setServices(new ArrayList<>());

        Visit savedVisit = visitRepository.save(visit);

        savedVisit
                .getEventLogs()
                .add(logEvent(savedVisit,"Visit created"));

        return savedVisit;
    }

    @Override
    public Visit updateVisit(Status status, long visitId, LocalDate bookedDate) {

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));

        if (visit.getStatus().equals(status)) {
            throw new EntityDuplicateException(String.format("Visit status already at [%s]", status));
        }

        if (bookedDate != null) {
            visit.setScheduleDate(bookedDate);
        }

        visit.setStatus(status);

        Visit savedVisit = visitRepository.saveAndFlush(visit);

        logEvent(savedVisit, String.format("Visit status changed to [%s]", status));

        return savedVisit;
    }

    @Override
    public void deleteVisit(long visitId) {
        Visit visitToDelete = visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));

        visitRepository.delete(visitToDelete);
    }

    private EventLog logEvent(Visit visit, String event) {
        EventLog eventLog = new EventLog(event, visit);
        return historyService.save(eventLog);
    }
}
