package com.ellipsis.homelabmonitor.repository;

import com.ellipsis.homelabmonitor.model.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    List<MonthlyReport> findByReportMonthAndReportYear(int month, int year);
    List<MonthlyReport> findByContainerNameOrderByReportYearAscReportMonthAsc(String name);
    List<MonthlyReport> findByReportYearOrderByReportMonthAsc(int year);
}
