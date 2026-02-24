package com.ellipsis.homelabmonitor.batch;

import com.ellipsis.homelabmonitor.model.ContainerInfo;
import com.ellipsis.homelabmonitor.model.DailyReport;
import com.ellipsis.homelabmonitor.repository.ContainerRepository;
import com.ellipsis.homelabmonitor.repository.DailyReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DailyReportJob {

    private final ContainerRepository containerRepository;
    private final DailyReportRepository dailyReportRepository;

    public DailyReportJob(ContainerRepository containerRepository,
                          DailyReportRepository dailyReportRepository) {
        this.containerRepository = containerRepository;
        this.dailyReportRepository = dailyReportRepository;
    }

    // Runs everyday at 2am
    @Scheduled(cron = "0 0 2 * * *")
    public void generateDailyReport() {
        System.out.println("Starting daily report job");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.atTime(23, 59, 59);

        List<ContainerInfo> snapshots = containerRepository.findByCheckedAtBetween(start,end);

        if (snapshots.isEmpty()) {
            System.out.println("No snapshots found for " + yesterday);
            return;
        }

        // Group snapshots by container name
        Map<String, List<ContainerInfo>> byContainer = snapshots.stream()
                .collect(Collectors.groupingBy(ContainerInfo::getName));

        for (Map.Entry<String, List<ContainerInfo>> entry : byContainer.entrySet()) {
            String containerName = entry.getKey();
            List<ContainerInfo> containerSnapshots = entry.getValue();

            int total = containerSnapshots.size();
            int running = (int) containerSnapshots.stream()
                    .filter(s -> s.getStatus().toLowerCase().startsWith("up"))
                    .count();

            double uptimePercent = total > 0 ? (running * 100.0) / total : 0;

            // Count incidents - transitions from running to not running
            int incidents = 0;
            for (int i = 1; i < containerSnapshots.size(); i++) {
                boolean wasRunning = containerSnapshots.get(i - 1).getStatus().toLowerCase().startsWith("up");
                boolean isRunning = containerSnapshots.get(i).getStatus().toLowerCase().startsWith("up");
                if (wasRunning && !isRunning) incidents++;
            }

            DailyReport report = DailyReport.builder()
                    .containerName(containerName)
                    .reportDate(yesterday)
                    .totalSnapshots(total)
                    .runningSnapshots(running)
                    .uptimePercentage(Math.round(uptimePercent * 10.0)/10.0)
                    .incidentCount(incidents)
                    .generatedAt(LocalDateTime.now())
                    .build();

            dailyReportRepository.save(report);
            System.out.println("Report saved for " + containerName + " - " + uptimePercent + "% uptime");
        }

        // Clean up raw snapshots older than 7 days
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        containerRepository.deleteByCheckedAtBefore(cutoff);
        System.out.println("Cleaned up snapshots older than 7 days");
    }

    // Testing
    public void runNow() {
        generateDailyReport();
    }
}
