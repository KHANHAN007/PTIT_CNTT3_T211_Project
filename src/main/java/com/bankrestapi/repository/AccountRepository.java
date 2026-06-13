package com.bankrestapi.repository;

import com.bankrestapi.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerUsernameOrderByIdAsc(String username);
    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllByIds(Collection<Long> ids);
}
