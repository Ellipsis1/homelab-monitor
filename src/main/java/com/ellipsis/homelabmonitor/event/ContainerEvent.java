package com.ellipsis.homelabmonitor.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerEvent {
    private String containerId;
    private String containerName;
    private String previousStatus;
    private String currentStatus;
    private LocalDateTime occurredAt;
    private String eventType; // CONTAINER_DOWN, CONTAINER_UP, CONTAINER_RESTARTED
}
