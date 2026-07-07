package com.gongsoop.member.repository;

import com.gongsoop.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByNameAndBirthdate(String name, LocalDate birthdate);

    Optional<Member> findByEmailAndNameAndBirthdate(String email, String name, LocalDate birthdate);
}
