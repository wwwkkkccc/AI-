package com.resumeai.repository;

import com.resumeai.model.UserAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户账户数据访问接口，提供user_accounts表的查询操作
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    // 根据用户名精确查询用户
    Optional<UserAccount> findByUsername(String username);
    // 分页查询所有用户，按ID倒序
    Page<UserAccount> findAllByOrderByIdDesc(Pageable pageable);
    // 根据用户名模糊搜索用户（忽略大小写），按ID倒序
    Page<UserAccount> findByUsernameContainingIgnoreCaseOrderByIdDesc(String username, Pageable pageable);
}
