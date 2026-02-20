package com.ellipsis.homelabmonitor.controller;

import com.ellipsis.homelabmonitor.model.ContainerInfo;
import com.ellipsis.homelabmonitor.repository.ContainerRepository;
import com.ellipsis.homelabmonitor.service.ContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://192.168.1.173:3002",
        "http://monitor.ellipsis.local"
})
@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    private final ContainerRepository containerRepository;
    private final ContainerService containerService;

    public ContainerController(ContainerRepository containerRepository,
                               ContainerService containerService) {
        this.containerRepository = containerRepository;
        this.containerService = containerService;
    }

    @GetMapping
    public List<ContainerInfo> getContainers() {
        return containerService.getCurrentContainers();
    }

    @GetMapping("/history")
    public List<ContainerInfo> getHistory(
            @RequestParam(defaultValue = "24") int hours) {
        return containerService.getHistory(hours);
    }

    @GetMapping("/history/{name}")
    public List<ContainerInfo> getHistoryByName(
            @PathVariable String name,
            @RequestParam(defaultValue = "24") int hours) {
        return containerService.getHistoryByName(name, hours);
    }

    @PostMapping("/restart/{name}")
    public ResponseEntity<String> restartContainer(@PathVariable String name) {
        containerService.restartContainer(name);
        return ResponseEntity.ok("Restart triggered for: " + name);
    }

    @PostMapping("/stop/{name}")
    public ResponseEntity<String> stopContainer(@PathVariable String name) {
        containerService.stopContainer(name);
        return ResponseEntity.ok("Stop triggered for: " + name);
    }

}
