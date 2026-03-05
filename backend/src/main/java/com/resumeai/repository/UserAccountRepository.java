package com.resumeai.repository;

import com.resumeai.model.UserAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    Page<UserAccount> findAllByOrderByIdDesc(Pageable pageable);
    Page<UserAccount> findByUsernameContainingIgnoreCaseOrderByIdDesc(String username, Pageable pageable);
}
