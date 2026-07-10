package com.gongsoop.inquiry.repository;

import com.gongsoop.inquiry.entity.Inquiry;
import com.gongsoop.inquiry.entity.InquiryComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {

    List<InquiryComment> findByInquiryOrderByCreatedAtAsc(Inquiry inquiry);

    Optional<InquiryComment> findByCommentIdAndInquiry(Long commentId, Inquiry inquiry);
}
