package com.santms.repository;
import com.santms.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    Optional<Configuration> findByConfigKey(String key);
    List<Configuration> findByOrganizationIdAndCategory(Long orgId, String category);
    Optional<Configuration> findByConfigKeyAndOrganizationId(String key, Long orgId);
}
