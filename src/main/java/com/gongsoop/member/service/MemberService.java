package com.gongsoop.member.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.global.exception.DuplicateEmailException;
import com.gongsoop.global.security.JwtProvider;
import com.gongsoop.global.security.TokenService;
import com.gongsoop.member.dto.request.FindEmailRequest;
import com.gongsoop.member.dto.request.LoginRequest;
import com.gongsoop.member.dto.request.ResetPasswordRequest;
import com.gongsoop.member.dto.request.SignupRequest;
import com.gongsoop.member.dto.request.UpdateCharacterRequest;
import com.gongsoop.member.dto.response.CharacterResponse;
import com.gongsoop.member.dto.response.LoginResponse;
import com.gongsoop.member.dto.response.MemberResponse;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
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
    private final TokenService tokenService;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                         JwtProvider jwtProvider, TokenService tokenService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
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

    public CharacterResponse updateCharacter(String email, UpdateCharacterRequest request) {
        Member member = memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        member.updateCharacter(request.characterId());
        return new CharacterResponse(member.getCharacterId());
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
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED);
        }

        String accessToken = jwtProvider.createAccessToken(member.getEmail(), member.getUserRole().name());
        String refreshToken = jwtProvider.createRefreshToken(member.getEmail());
        tokenService.saveRefreshToken(member.getEmail(), refreshToken, jwtProvider.getRemainingTimeMs(refreshToken));
        return new LoginResponse("Bearer", accessToken, refreshToken, MemberResponse.from(member));
    }

    public void logout(String email, String accessToken) {
        tokenService.blacklistAccessToken(accessToken, jwtProvider.getRemainingTimeMs(accessToken));
        tokenService.deleteRefreshToken(email);
    }

    @Transactional(readOnly = true)
    public String refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED);
        }
        Claims claims = jwtProvider.parseToken(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED);
        }
        String email = claims.getSubject();
        if (!tokenService.matchesRefreshToken(email, refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED);
        }
        Member member = memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.UNAUTHORIZED));
        return jwtProvider.createAccessToken(member.getEmail(), member.getUserRole().name());
    }
}
