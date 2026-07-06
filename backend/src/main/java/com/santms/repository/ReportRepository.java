package com.santms.repository;
import com.santms.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);
    List<Report> findByOrganizationIdAndType(Long orgId, Report.ReportType type);
}
