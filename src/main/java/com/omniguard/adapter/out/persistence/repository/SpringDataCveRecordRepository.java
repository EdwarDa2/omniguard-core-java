package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.CveRecordJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCveRecordRepository extends JpaRepository<CveRecordJpaEntity, String> {

    @Query(
            value = """
            SELECT r.* FROM nvd.cve_records r
            INNER JOIN nvd.asset_cve_matches m ON r.cve_id = m.cve_id
            WHERE m.asset_id = :assetId
            ORDER BY r.cvss_score DESC NULLS LAST
            """,
            nativeQuery = true
    )
    List<CveRecordJpaEntity> findVulnerabilitiesByAssetId(@Param("assetId") Long assetId);

    @Query(
            value = "SELECT COUNT(DISTINCT asset_id) FROM nvd.asset_cve_matches",
            nativeQuery = true
    )
    long countVulnerableAssets();
}
