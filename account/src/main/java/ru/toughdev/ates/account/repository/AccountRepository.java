package ru.toughdev.ates.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.toughdev.ates.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, String> {

    Account getByUserId(Long userId);

    @Modifying
    @Query("UPDATE Account acc SET acc.balance = :balance where acc.userId = :userId")
    int setBalanceForUser(Long balance, Long userId);
}
