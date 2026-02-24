package com.ellipsis.homelabmonitor.repository;

import com.ellipsis.homelabmonitor.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    List<DailyReport> findByReportDateBetween(LocalDate start, LocalDate end);
    List<DailyReport> findByReportDate(LocalDate date);
    List<DailyReport> findByContainerNameAndReportDateBetween(String name, LocalDate start, LocalDate end);
}
