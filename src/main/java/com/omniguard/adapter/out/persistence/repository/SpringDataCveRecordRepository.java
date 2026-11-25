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

    @Query(
            value = """
            SELECT DISTINCT m.cve_id FROM nvd.cpe_matches m
            WHERE m.cpe_name = :cpeName OR m.cpe_name LIKE CONCAT(:cpePrefix, '%')
            LIMIT :maxResults
            """,
            nativeQuery = true
    )
    List<String> findCveIdsByCpe(@Param("cpeName") String cpeName, @Param("cpePrefix") String cpePrefix, @Param("maxResults") int maxResults);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(
            value = """
            INSERT INTO nvd.asset_cve_matches (asset_id, service_id, cve_id, cpe_name, matched_at)
            SELECT :assetId, :serviceId, :cveId, :cpeName, CURRENT_TIMESTAMP
            WHERE NOT EXISTS (
                SELECT 1 FROM nvd.asset_cve_matches
                WHERE service_id = :serviceId AND cve_id = :cveId
            )
            """,
            nativeQuery = true
    )
    void insertMatch(
            @Param("assetId") Long assetId,
            @Param("serviceId") Long serviceId,
            @Param("cveId") String cveId,
            @Param("cpeName") String cpeName
    );
}
