package com.ellipsis.homelabmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String containerName;
    private int reportMonth;
    private int reportYear;
    private double averageUptimePercentage;
    private int totalIncidents;
    private int daysWithIncidents;
    private int totalDaysReported;
    private LocalDateTime generatedAt;
}
