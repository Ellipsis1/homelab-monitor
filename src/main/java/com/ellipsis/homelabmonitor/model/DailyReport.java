package com.ellipsis.homelabmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String containerName;
    private LocalDate reportDate;
    private int totalSnapshots;
    private int runningSnapshots;
    private double uptimePercentage;
    private int incidentCount;
    private LocalDateTime generatedAt;
}
