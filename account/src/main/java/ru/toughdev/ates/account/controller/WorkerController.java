package ru.toughdev.ates.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.toughdev.ates.account.dto.WorkerPaymentsDto;
import ru.toughdev.ates.account.repository.AccountRepository;
import ru.toughdev.ates.account.repository.PaymentRepository;
import ru.toughdev.ates.account.repository.TaskRepository;
import ru.toughdev.ates.account.repository.UserRepository;
import ru.toughdev.ates.account.security.JwtUser;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/account/worker", produces = MediaType.APPLICATION_JSON_VALUE)
public class WorkerController {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    private static final String PAYMENT_MESSAGE = "Таск № %s. %s %s тугриков. Дата: %s";

    @PreAuthorize("hasAuthority('worker')")
    @GetMapping("/current-payments")
    public @ResponseBody WorkerPaymentsDto getMyPayments(Authentication authentication) {
        var login = ((JwtUser) authentication.getPrincipal()).getUsername();
        var user = userRepository.getByLogin(login);

        var balance = accountRepository.getByUserId(user.getId()).getBalance();
        var payments = paymentRepository.findCurrentPaymentsByUserId(user.getId());

        Long paymentsTotal = 0L;
        for (var payment : payments) {
            paymentsTotal += payment.getAmount() * (payment.getType().equals("fee") ? -1 : 1);
        }

        var paymentLogs = payments.stream()
                .map(pay -> String.format(PAYMENT_MESSAGE, pay.getTaskId(),
                        pay.getType().equals("fee") ? "Списано" : "Начислено",
                        pay.getAmount(), pay.getDateTime())
                )
                .collect(Collectors.toList());

        return new WorkerPaymentsDto(balance + paymentsTotal, paymentLogs);
    }
}
