package com.gongsoop.jangwon.repository;

import com.gongsoop.jangwon.entity.JangwonApplication;
import com.gongsoop.jangwon.entity.JangwonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JangwonApplicationRepository extends JpaRepository<JangwonApplication, Long> {

    Page<JangwonApplication> findByStatusAndIsVisible(
            JangwonStatus status,
            Integer isVisible,
            Pageable pageable
    );

    Page<JangwonApplication> findByMemberIdOrderByAppliedAtDesc(
            Long memberId,
            Pageable pageable
    );

    boolean existsByMemberIdAndStatus(Long memberId, JangwonStatus status);

    @Query("""
            SELECT j
            FROM JangwonApplication j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(j.displayNickname) LIKE LOWER(:keyword)
                   OR LOWER(j.displayName) LIKE LOWER(:keyword)
                   OR LOWER(j.characterName) LIKE LOWER(:keyword))
            """)
    Page<JangwonApplication> searchForAdmin(
            @Param("status") JangwonStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}