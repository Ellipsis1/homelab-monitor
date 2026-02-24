package com.ellipsis.homelabmonitor.controller;

import com.ellipsis.homelabmonitor.batch.DailyReportJob;
import com.ellipsis.homelabmonitor.batch.MonthlyReportJob;
import com.ellipsis.homelabmonitor.model.DailyReport;
import com.ellipsis.homelabmonitor.model.MonthlyReport;
import com.ellipsis.homelabmonitor.repository.DailyReportRepository;
import com.ellipsis.homelabmonitor.repository.MonthlyReportRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final DailyReportRepository dailyReportRepository;
    private final DailyReportJob dailyReportJob;
    private final MonthlyReportRepository monthlyReportRepository;
    private final MonthlyReportJob monthlyReportJob;

    public ReportController(DailyReportRepository dailyReportRepository,
                            DailyReportJob dailyReportJob,
                            MonthlyReportRepository monthlyReportRepository,
                            MonthlyReportJob monthlyReportJob) {
        this.dailyReportRepository = dailyReportRepository;
        this.dailyReportJob = dailyReportJob;
        this.monthlyReportRepository = monthlyReportRepository;
        this.monthlyReportJob = monthlyReportJob;
    }

    // Get all reports for a date range
    @GetMapping
    public List<DailyReport> getReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return dailyReportRepository.findByReportDateBetween(start, end);
    }

    // Get reports for a specific date
    @GetMapping("/date/{date}")
    public List<DailyReport> getReportsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyReportRepository.findByReportDate(date);
    }

    // Get reports for a specific container
    @GetMapping("/container/{name}")
    public List<DailyReport> getReportsByContainer(
            @PathVariable String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return dailyReportRepository.findByContainerNameAndReportDateBetween(name, start, end);
    }

    // Manual Trigger for testing
    @PostMapping("/generate")
    public ResponseEntity<String> generateReport() {
        dailyReportJob.runNow();
        return ResponseEntity.ok().body("Report Generation Triggered");
    }

    // Get reports for a specific month and year
    @GetMapping("/monthly/{year}/{month}")
    public List<MonthlyReport> getMonthlyReports(
            @PathVariable int year,
            @PathVariable int month) {
        return monthlyReportRepository.findByReportMonthAndReportYear(month, year);
    }

    // Get yearly report
    @GetMapping("/monthly/{year}")
    public List<MonthlyReport> getMonthlyReportsByYear(@PathVariable int year) {
        return monthlyReportRepository.findByReportYearOrderByReportMonthAsc(year);
    }

    @PostMapping("/generate/monthly")
    public ResponseEntity<String> generateMonthlyReport() {
        monthlyReportJob.runNow();
        return ResponseEntity.ok().body("Monthly Report Generation Triggered");
    }
}
