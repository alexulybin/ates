package ru.toughdev.ates.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.toughdev.ates.analytics.dto.UserTotalPayment;
import ru.toughdev.ates.analytics.model.Payment;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query(value = "SELECT SUM(p.amount) " +
            "FROM PAYMENTS p " +
            "WHERE CAST(p.date_time AS date) = CURRENT_DATE() AND p.type IN ('fee', 'reward')",
            nativeQuery = true)
    Long getCurrentTopManagementTotalPayment();

    @Query(value = "SELECT p.user_id as userId, SUM (CASE WHEN (p.type = 'fee') THEN p.amount * -1 ELSE p.amount END) as totalAmount " +
            "FROM PAYMENTS p " +
            "WHERE CAST(p.date_time AS date) = CURRENT_DATE() AND p.type IN ('fee', 'reward') " +
            "GROUP BY p.user_id",
            nativeQuery = true)
    List<UserTotalPayment> getCurrentTotalPaymentsForAllUsers();
}
