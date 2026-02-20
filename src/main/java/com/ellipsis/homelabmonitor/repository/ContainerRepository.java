package com.ellipsis.homelabmonitor.repository;

import com.ellipsis.homelabmonitor.model.ContainerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerInfo, Long> {
    List<ContainerInfo> findByCheckedAtAfter(LocalDateTime since);
    List<ContainerInfo> findByNameAndCheckedAtAfter(String name, LocalDateTime since);
}
