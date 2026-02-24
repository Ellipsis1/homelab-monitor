package com.ellipsis.homelabmonitor.batch;

import com.ellipsis.homelabmonitor.model.DailyReport;
import com.ellipsis.homelabmonitor.model.MonthlyReport;
import com.ellipsis.homelabmonitor.repository.DailyReportRepository;
import com.ellipsis.homelabmonitor.repository.MonthlyReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MonthlyReportJob {

    private final DailyReportRepository dailyReportRepository;
    private final MonthlyReportRepository monthlyReportRepository;

    public MonthlyReportJob(DailyReportRepository dailyReportRepository,
                            MonthlyReportRepository monthlyReportRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.monthlyReportRepository = monthlyReportRepository;
    }

    // Runs at 3am on the first day of every month
    @Scheduled(cron = "0 0 3 1 * *")
    public void generateMonthlyReport() {
        System.out.println("Starting monthly report generation");

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate start = lastMonth.withDayOfMonth(1);
        LocalDate end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        List<DailyReport> dailyReports = dailyReportRepository.findByReportDateBetween(start, end);

        if (dailyReports.isEmpty()) {
            System.out.println("No daily reports found for " + lastMonth.getMonth());
            return;
        }

        Map<String, List<DailyReport>> byContainer = dailyReports.stream()
                .collect(Collectors.groupingBy(DailyReport::getContainerName));

        for (Map.Entry<String, List<DailyReport>> entry : byContainer.entrySet()) {
            String containerName = entry.getKey();
            List<DailyReport> reports = entry.getValue();

            double avgUptime = reports.stream()
                    .mapToDouble(DailyReport::getUptimePercentage)
                    .average()
                    .orElse(0);

            int totalIncidents = reports.stream()
                    .mapToInt(DailyReport::getIncidentCount)
                    .sum();

            int daysWithIncidents = (int) reports.stream()
                    .filter(r -> r.getIncidentCount() > 0)
                    .count();

            MonthlyReport report = MonthlyReport.builder()
                    .containerName(containerName)
                    .reportMonth(lastMonth.getMonthValue())
                    .reportYear(lastMonth.getYear())
                    .averageUptimePercentage(Math.round(avgUptime * 10.0) / 10.0)
                    .totalIncidents(totalIncidents)
                    .daysWithIncidents(daysWithIncidents)
                    .totalDaysReported(reports.size())
                    .generatedAt(LocalDateTime.now())
                    .build();

            monthlyReportRepository.save(report);
            System.out.println("Monthly report saved for " + containerName);
        }
    }

    // Testing
    public void runNow() {
        generateMonthlyReport();
    }
}