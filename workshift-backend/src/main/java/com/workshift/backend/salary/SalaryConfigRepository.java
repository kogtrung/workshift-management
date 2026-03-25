package com.workshift.backend.salary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.SalaryConfig;

import java.util.List;

@Repository
public interface SalaryConfigRepository extends JpaRepository<SalaryConfig, Long> {
    List<SalaryConfig> findByGroupIdOrderByEffectiveDateDesc(Long groupId);

    List<SalaryConfig> findByGroupIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Long groupId, java.time.LocalDate date);
}
