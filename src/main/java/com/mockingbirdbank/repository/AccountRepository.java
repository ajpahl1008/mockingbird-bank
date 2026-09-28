package com.mockingbirdbank.repository;

import com.mockingbirdbank.model.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByHolderIdOrderByAccountTypeAscIdAsc(Long holderId);
}
