package ru.toughdev.ates.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.toughdev.ates.account.dto.TotalPaymentsDto;
import ru.toughdev.ates.account.repository.PaymentRepository;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/account/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    private final PaymentRepository paymentRepository;

    @PreAuthorize("hasAuthority('admin') or hasAuthority('accountant')")
    @GetMapping("/total-payments")
    public @ResponseBody TotalPaymentsDto getStatistics() {
        var payments = paymentRepository.getCurrentTopManagementTotalPayment();
        return new TotalPaymentsDto(payments);
    }
}
