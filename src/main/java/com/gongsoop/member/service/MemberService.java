package com.gongsoop.member.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.global.exception.DuplicateEmailException;
import com.gongsoop.global.security.JwtProvider;
import com.gongsoop.member.dto.request.FindEmailRequest;
import com.gongsoop.member.dto.request.LoginRequest;
import com.gongsoop.member.dto.request.ResetPasswordRequest;
import com.gongsoop.member.dto.request.SignupRequest;
import com.gongsoop.member.dto.response.LoginResponse;
import com.gongsoop.member.dto.response.MemberResponse;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        memberRepository.save(new Member(request.name(), request.birthdate(), request.email(), hashedPassword));
    }

    @Transactional(readOnly = true)
    public String findEmail(FindEmailRequest request) {
        Member member = memberRepository.findByNameAndBirthdate(request.name(), request.birthdate())
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "일치하는 회원 정보가 없습니다", HttpStatus.NOT_FOUND));
        return member.getEmail();
    }

    public void withdraw(String email) {
        Member member = memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        member.delete();
    }

    public void resetPassword(ResetPasswordRequest request) {
        Member member = memberRepository.findByEmailAndNameAndBirthdate(request.email(), request.name(), request.birthdate())
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "일치하는 회원 정보가 없습니다", HttpStatus.NOT_FOUND));
        member.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtProvider.createToken(member.getEmail(), member.getUserRole().name());
        return new LoginResponse("Bearer", token, MemberResponse.from(member));
    }
}
