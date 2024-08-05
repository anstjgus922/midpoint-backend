package com.solucitation.midpoint_backend.domain.member.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solucitation.midpoint_backend.domain.file.service.S3Service;
import com.solucitation.midpoint_backend.domain.member.dto.*;
import com.solucitation.midpoint_backend.domain.member.exception.PasswordMismatchException;
import com.solucitation.midpoint_backend.domain.member.service.MemberService;
import com.solucitation.midpoint_backend.global.auth.JwtTokenProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController2 {
    private final MemberService memberService;
    private final S3Service s3Service;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * 회원 프로필 정보를 가져옵니다.
     *
     * @param authentication 인증 정보
     * @return 회원의 이름을 포함한 응답
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMemberInfo(Authentication authentication) {
        String email = authentication.getName();
        MemberProfileResponseDto memberProfile = memberService.getMemberProfile(email);
        return ResponseEntity.ok(memberProfile);
    }

    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestPart(value = "profileUpdateRequestDto", required = false) String profileUpdateRequestDtoJson,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        if (profileUpdateRequestDtoJson == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "profile_update_empty_dto", "message", "프로필 수정 요청 dto가 비었습니다."));
        }
        ProfileUpdateRequestDto profileUpdateRequestDto = objectMapper.readValue(profileUpdateRequestDtoJson, ProfileUpdateRequestDto.class);
        Set<ConstraintViolation<ProfileUpdateRequestDto>> violations = validator.validate(profileUpdateRequestDto);
        if (!violations.isEmpty()) {
            List<ValidationErrorResponse.FieldError> fieldErrors = violations.stream()
                    .map(violation -> new ValidationErrorResponse.FieldError(violation.getPropertyPath().toString(), violation.getMessage()))
                    .collect(Collectors.toList());
            ValidationErrorResponse errorResponse = new ValidationErrorResponse(fieldErrors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        String email = authentication.getName();
        return validateAndUpdate(email, profileUpdateRequestDto, profileImage);
    }

    private ResponseEntity<?> validateAndUpdate(String email, @Valid ProfileUpdateRequestDto profileUpdateRequestDto, MultipartFile profileImage) throws IOException {
        memberService.updateMember(email, profileUpdateRequestDto, profileImage);
        return ResponseEntity.ok(Map.of("message", "프로필 수정이 성공적으로 완료되었습니다."));
    }


    /**
     * 비밀번호를 확인합니다.
     *
     * @param passwordVerifyRequestDto 비밀번호 확인 요청 DTO
     * @param authentication           인증 정보
     * @return 비밀번호가 일치하면 새로운 액세스 토큰을 반환, 일치하지 않으면 오류 메시지 반환
     */
    @PostMapping("/verify-pw")
    public ResponseEntity<?> verifyPassword(@RequestBody @Valid PasswordVerifyRequestDto passwordVerifyRequestDto, Authentication authentication) {
        String email = authentication.getName();
        try {
            memberService.verifyPassword(email, passwordVerifyRequestDto);
            String tokenForDelete = jwtTokenProvider.createShortLivedTokenWithPurpose(authentication, "delete");
            String tokenForResetPassword = jwtTokenProvider.createShortLivedTokenWithPurpose(authentication, "reset-password");

            return ResponseEntity.ok(Map.of(
                    "tokenForResetPassword", Map.of(
                            "grantType", "Bearer",
                            "X-Reset-Password-Token", tokenForResetPassword
                    ),
                    "tokenForDelete", Map.of(
                            "grantType", "Bearer",
                            "X-Delete-Token", tokenForDelete
                    )
            ));
        } catch (PasswordMismatchException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "password_mismatch", "message", e.getMessage()));
        }
    }

    /**
     * 회원 탈퇴를 처리합니다.
     *
     * @param deleteToken    비밀번호 확인했고, 탈퇴하겠다는 인증 토큰
     * @param authentication 인증 객체
     * @return 회원 탈퇴 성공 메시지 또는 오류 메시지
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMember(@RequestHeader("X-Delete-Token") String deleteToken, @RequestHeader("X-Refresh-Token") String refreshTokenHeader, Authentication authentication) {
        ResponseEntity<?> validationResponse = validateTokenAndEmail(deleteToken, "delete", authentication);
        if (validationResponse != null) { // 에러 응답을 가진 경우
            return validationResponse; // 에러 응답을 리턴
        }
        String tokenEmail = jwtTokenProvider.extractEmailFromToken(jwtTokenProvider.resolveToken(deleteToken));
        String profileImgUrl = memberService.deleteMember(tokenEmail); // 프로필 이미지와 멤버 엔티티 삭제
        log.info("profileImgUrl는 by test? " + profileImgUrl);
        String refreshToken = jwtTokenProvider.resolveToken(refreshTokenHeader); // Authorization 헤더에서 Bearer 토큰을 제외한 Refresh Token만 추출
        memberService.logoutMember(refreshToken); // 로그아웃 처리
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 성공적으로 완료되었습니다."));
    }


    private ResponseEntity<?> validateTokenAndEmail(String BFtoken, String action, Authentication authentication) {
        String token = jwtTokenProvider.resolveToken(BFtoken);
        if (!jwtTokenProvider.validateTokenByPwConfirm(token, action)) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized", "message", "회원을 탈퇴할 권한이 없습니다"));
        }
        String tokenEmail = jwtTokenProvider.extractEmailFromToken(token); // 토큰으로부터 이메일 추출
        String authEmail = authentication.getName(); // 인증 객체로부터 이메일 추출

        if (!tokenEmail.equals(authEmail)) { // 토큰으로부터 추출한 이메일과 인증 객체로부터 추출한 이메일이 동일한지 비교
            return ResponseEntity.status(403).body(Map.of("error", "forbidden", "message", "토큰의 이메일과 인증된 이메일이 일치하지 않습니다."));
        }
        return null;
    }

    /**
     * 사용자를 로그아웃시키고 Refresh Token을 무효화합니다.
     *
     * @param refreshTokenHeader Authorization 헤더에 포함된 Refresh Token
     * @param authentication     인증 정보
     * @return 로그아웃 성공 메시지 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutMember(@RequestHeader("logout-token") String refreshTokenHeader, Authentication authentication) {
        String refreshToken = jwtTokenProvider.resolveToken(refreshTokenHeader); // Authorization 헤더에서 Bearer 토큰을 제외한 Refresh Token만 추출

        // 현재 인증된 사용자의 정보 가져오기
        if (authentication != null) {
            String authenticatedEmail = authentication.getName(); // 인증된 사용자의 이메일

            // Refresh Token에서 이메일 추출
            String tokenEmail = jwtTokenProvider.extractEmailFromToken(refreshToken);
            if (!authenticatedEmail.equals(tokenEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그아웃할 권한이 없습니다."));
            }

            memberService.logoutMember(refreshToken);
            return ResponseEntity.ok(Map.of("message", "로그아웃에 성공하였습니다."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인된 사용자가 없습니다."));
        }
    }
}
