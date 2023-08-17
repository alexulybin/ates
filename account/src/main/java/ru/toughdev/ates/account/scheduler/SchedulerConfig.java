package ru.toughdev.ates.account.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.toughdev.ates.account.kafka.MessageProducer;
import ru.toughdev.ates.account.kafka.event.AccountEvent;
import ru.toughdev.ates.account.kafka.event.PaymentEvent;
import ru.toughdev.ates.account.repository.AccountRepository;
import ru.toughdev.ates.account.repository.PaymentRepository;
import ru.toughdev.ates.account.repository.UserRepository;

import javax.transaction.Transactional;
import java.util.UUID;

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
    public void calculateBalance() throws JsonProcessingException {
        var payments = paymentRepository.getCurrentTotalPaymentsForAllUsers();
        for (var payment : payments) {
            var userId = payment.getUserId();
            var user = userRepository.getById(userId);
            var account = accountRepository.getByUserId(userId);
            var userBalance = account.getBalance() + payment.getTotalAmount();
            long newBalance = 0L;

            if (userBalance <= 0) {
                newBalance = userBalance;
                accountRepository.setBalanceForUser(userBalance, userId);
            } else {
                accountRepository.setBalanceForUser(newBalance, userId);

                // mock для отправки письма на email
                log.info("Payment notification sent to user " + userId);

                // на будущее, пока никем не консьюмится
                sendAccountEvent("account-lifecycle", "DailyRewardPaid", user.getPublicId(), userBalance);
            }

            sendAccountEvent("account-stream", "AccountUpdated", user.getPublicId(), newBalance);
        }
    }

    private void sendAccountEvent(String topic, String type, UUID userPublicId, Long balance) throws JsonProcessingException {
        var accountEvent = AccountEvent.builder()
                .eventType(type)
                .userPublicId(userPublicId)
                .balance(balance)
                .build();

        messageProducer.sendMessage(accountEvent, topic);
    }
}
