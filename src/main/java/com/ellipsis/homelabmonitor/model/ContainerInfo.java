package com.ellipsis.homelabmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "container_info")
public class ContainerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbID;

    private String id;
    private String name;
    private String status;
    private String image;
    private LocalDateTime checkedAt;
}
