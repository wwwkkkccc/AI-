package com.resumeai.repository;

import com.resumeai.model.RegisterIpDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisterIpDailyRepository extends JpaRepository<RegisterIpDaily, Long> {
    Optional<RegisterIpDaily> findByIpAndDayDate(String ip, LocalDate dayDate);
}
