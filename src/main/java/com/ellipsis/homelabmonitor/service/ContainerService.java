package com.ellipsis.homelabmonitor.service;

import com.ellipsis.homelabmonitor.event.ContainerEvent;
import com.ellipsis.homelabmonitor.kafka.ContainerEventProducer;
import com.ellipsis.homelabmonitor.model.ContainerInfo;
import com.ellipsis.homelabmonitor.repository.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContainerService {
    private DockerClient dockerClient;

    private final ContainerRepository containerRepository;
    private final ContainerEventProducer eventProducer;

    // Tracks last known status of each container in memory
    private final Map<String, String> lastKnownStatus = new HashMap<>();

    public ContainerService(ContainerRepository containerRepository,
                            ContainerEventProducer eventProducer) {
        this.containerRepository = containerRepository;
        this.eventProducer = eventProducer;
    }

    @PostConstruct
    public void init() {
        String host = System.getenv("DOCKER_HOST");
        if (host == null || host.isEmpty()) host = "tcp://localhost:2375";

        System.out.println("Connecting to Docker at: " + host);

        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(host)
                .build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public List<ContainerInfo> fetchAndSave() {
        List<ContainerInfo> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .map(container -> ContainerInfo.builder()
                        .id(container.getId().substring(0, 12))
                        .name(container.getNames()[0].replaceFirst("^/", ""))
                        .status(container.getStatus())
                        .image(container.getImage())
                        .checkedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        containerRepository.saveAll(containers);
        detectStatusChanges(containers);
        return containers;
    }

    public List<ContainerInfo> getCurrentContainers() {
        return dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .map(container -> ContainerInfo.builder()
                        .id(container.getId().substring(0, 12))
                        .name(container.getNames()[0].replaceFirst("^/", ""))
                        .status(container.getStatus())
                        .image(container.getImage())
                        .checkedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    private void detectStatusChanges(List<ContainerInfo> containers) {
        for (ContainerInfo container : containers) {
            String name = container.getName();
            String currentStatus = normalizeStatus(container.getStatus());
            String previousStatus = lastKnownStatus.get(name);

            if (previousStatus == null) {
                lastKnownStatus.put(name, currentStatus);
                continue;
            }

            if (!currentStatus.equals(previousStatus)) {
                boolean meaningfulChange =
                        (previousStatus.equals("running") && currentStatus.equals("exited")) ||
                                (previousStatus.equals("exited") && currentStatus.equals("running"));

                if (meaningfulChange) {
                    String eventType = currentStatus.equals("running")
                            ? "CONTAINER_UP" : "CONTAINER_DOWN";

                    ContainerEvent event = ContainerEvent.builder()
                            .containerId(container.getId())
                            .containerName(name)
                            .previousStatus(previousStatus)
                            .currentStatus(currentStatus)
                            .occurredAt(LocalDateTime.now())
                            .eventType(eventType)
                            .build();

                    eventProducer.publishEvent(event);
                }
            }

            lastKnownStatus.put(name, currentStatus);
        }
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) return "unknown";
        String lower = rawStatus.toLowerCase();
        if (lower.startsWith("up")) return "running";
        if (lower.startsWith("restarting")) return "running";
        if (lower.startsWith("exited")) return "exited";
        if (lower.startsWith("paused")) return "paused";
        return "unknown";
    }

    @Scheduled(fixedRateString = "${monitor.poll-interval-ms:60000}")
    public void scheduledpoll() {
        try {
            fetchAndSave();
        } catch (Exception e) {
            System.out.println("Poll failed - database may be unavailable: " + e.getMessage());
        }
    }

    public List<ContainerInfo> getHistory(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return containerRepository.findByCheckedAtAfter(since);
    }

    public List<ContainerInfo> getHistoryByName(String name, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return containerRepository.findByNameAndCheckedAtAfter(name, since);
    }

    public void restartContainer(String name) {
        try {
            dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .filter(c -> c.getNames()[0].replaceFirst("^/", "").equals(name))
                    .findFirst()
                    .ifPresentOrElse(
                            container -> {
                                dockerClient.restartContainerCmd(container.getId()).exec();
                                System.out.println("Restarted container: " + name);

                                ContainerEvent event = ContainerEvent.builder()
                                        .containerId(container.getId().substring(0, 12))
                                        .containerName(name)
                                        .previousStatus("running")
                                        .currentStatus("running")
                                        .occurredAt(LocalDateTime.now())
                                        .eventType("CONTAINER_RESTARTED")
                                        .build();

                                eventProducer.publishEvent(event);
                            },
                            () -> System.out.println("Container not found: " + name)
                    );
        } catch (Exception e) {
            System.out.println("Failed to restart container: " + name + " - " + e.getMessage());
        }
    }

    public void stopContainer(String name) {
        try {
            dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .filter(c -> c.getNames()[0].replaceFirst("^/", "").equals(name))
                    .findFirst()
                    .ifPresentOrElse(
                            container -> {
                                dockerClient.stopContainerCmd(container.getId()).exec();
                                System.out.println("Stopped container: " + name);

                                ContainerEvent event = ContainerEvent.builder()
                                        .containerId(container.getId().substring(0, 12))
                                        .containerName(name)
                                        .previousStatus("running")
                                        .currentStatus("exited")
                                        .occurredAt(LocalDateTime.now())
                                        .eventType("CONTAINER_STOPPED")
                                        .build();

                                eventProducer.publishEvent(event);
                            },
                            () -> System.out.println("Container not found: " + name)
                    );
        } catch (Exception e) {
            System.out.println("Failed to stop container: " + name + " — " + e.getMessage());
        }
    }
}