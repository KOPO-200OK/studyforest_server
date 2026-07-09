package com.gongsoop.notice.repository;

import com.gongsoop.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByIsPublished(Integer isPublished, Pageable pageable);

    Optional<Notice> findByNoticeIdAndIsPublished(Long noticeId, Integer isPublished);

    @Query("""
            SELECT n
            FROM Notice n
            WHERE (:keyword IS NULL
                   OR LOWER(n.title) LIKE LOWER(:keyword))
              AND (:isPublished IS NULL OR n.isPublished = :isPublished)
            """)
    Page<Notice> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("isPublished") Integer isPublished,
            Pageable pageable
    );
}