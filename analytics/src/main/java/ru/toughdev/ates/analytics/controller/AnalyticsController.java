package ru.toughdev.ates.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.toughdev.ates.analytics.dto.TotalPaymentsDto;
import ru.toughdev.ates.analytics.repository.AccountRepository;
import ru.toughdev.ates.analytics.repository.PaymentRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class AnalyticsController {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/total-payments")
    public @ResponseBody TotalPaymentsDto getCurrentTopManagementTotalPayment() {
        var payments = paymentRepository.getCurrentTopManagementTotalPayment();
        return new TotalPaymentsDto(payments);
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/minus-balanced-popugs")
    public @ResponseBody Integer getMinusBalancedPopugsCount() {
        int count = 0;

        var payments = paymentRepository.getCurrentTotalPaymentsForAllUsers();
        for (var payment : payments) {
            var userId = payment.getUserId();
            var account = accountRepository.getByUserId(userId);
            var userBalance = account.getBalance() + payment.getTotalAmount();

            if (userBalance < 0) {
                count++;
            }
        }

        return count;
    }
}
