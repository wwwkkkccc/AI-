package com.resumeai.repository;

import com.resumeai.model.RegisterIpDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 每日IP注册统计数据访问接口，提供register_ip_daily表的查询操作
 */
public interface RegisterIpDailyRepository extends JpaRepository<RegisterIpDaily, Long> {
    // 根据IP和日期查询当天的注册统计记录
    Optional<RegisterIpDaily> findByIpAndDayDate(String ip, LocalDate dayDate);
}
