package ru.toughdev.ates.account.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.toughdev.ates.account.kafka.MessageProducer;
import ru.toughdev.ates.account.repository.AccountRepository;
import ru.toughdev.ates.account.repository.PaymentRepository;
import ru.toughdev.ates.account.repository.UserRepository;
import ru.toughdev.ates.event.account.AccountBalanceUpdatedEventV1;
import ru.toughdev.ates.event.account.DailyRewardPaidEventV1;

import javax.transaction.Transactional;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private final MessageProducer messageProducer;

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void calculateBalance() {
        var payments = paymentRepository.getCurrentTotalPaymentsForAllUsers();
        for (var payment : payments) {
            var userId = payment.getUserId();
            var user = userRepository.getById(userId);
            var account = accountRepository.getByUserId(userId);
            var dailyBalance = account.getBalance() + payment.getTotalAmount();

            var newBalance = dailyBalance < 0 ? dailyBalance : 0L;
            accountRepository.setBalanceForUser(newBalance, userId);

            var accountBalanceEvent = AccountBalanceUpdatedEventV1.newBuilder()
                    .setEventType("AccountBalanceUpdated")
                    .setUserPublicId(user.getPublicId().toString())
                    .setBalance(newBalance)
                    .build();
            messageProducer.sendMessage(accountBalanceEvent, "account-stream");

            if (dailyBalance > 0) {
                // mock для отправки письма на email
                log.info("Payment notification sent to user " + userId);

                var dailyRewardEvent = DailyRewardPaidEventV1.newBuilder()
                        .setEventType("DailyRewardPaid")
                        .setUserPublicId(user.getPublicId().toString())
                        .setAmount(dailyBalance)
                        .build();
                messageProducer.sendMessage(dailyRewardEvent, "account-lifecycle");
            }
        }
    }
}
