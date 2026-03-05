package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 每日IP注册次数统计表，用于限制同一IP每天的注册次数，防止恶意注册
 */
@Entity
@Table(
        name = "register_ip_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_register_ip_day", columnNames = {"ip", "day_date"})
)
public class RegisterIpDaily {
    // 自增主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 注册来源IP地址
    @Column(name = "ip", nullable = false, length = 64)
    private String ip;

    // 统计日期
    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    // 该IP当天的注册次数
    @Column(name = "register_count", nullable = false)
    private Integer registerCount;

    // 最后更新时间
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDate getDayDate() {
        return dayDate;
    }

    public void setDayDate(LocalDate dayDate) {
        this.dayDate = dayDate;
    }

    public Integer getRegisterCount() {
        return registerCount;
    }

    public void setRegisterCount(Integer registerCount) {
        this.registerCount = registerCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
