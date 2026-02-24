package com.ellipsis.homelabmonitor.repository;

import com.ellipsis.homelabmonitor.model.ContainerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerInfo, Long> {
    List<ContainerInfo> findByCheckedAtAfter(LocalDateTime since);
    List<ContainerInfo> findByNameAndCheckedAtAfter(String name, LocalDateTime since);
    List<ContainerInfo> findByCheckedAtBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Transactional
    @Query("DELETE FROM ContainerInfo c WHERE c.checkedAt < :cutoff")
    void deleteByCheckedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
