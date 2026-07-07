package com.gongsoop.member.service;

import com.gongsoop.global.exception.DuplicateEmailException;
import com.gongsoop.member.dto.request.SignupRequest;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        memberRepository.save(new Member(request.name(), request.birthdate(), request.email(), hashedPassword));
    }
}
