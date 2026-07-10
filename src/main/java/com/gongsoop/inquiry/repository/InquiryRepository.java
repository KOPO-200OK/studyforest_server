package com.gongsoop.inquiry.repository;

import com.gongsoop.inquiry.entity.Inquiry;
import com.gongsoop.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByMember(Member member, Pageable pageable);

    Page<Inquiry> findByIsSecretFalse(Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE (:keyword IS NULL OR i.title LIKE :keyword OR i.content LIKE :keyword)")
    Page<Inquiry> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
