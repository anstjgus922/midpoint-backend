package com.solucitation.midpoint_backend.domain.member.api;

import com.solucitation.midpoint_backend.domain.member.dto.SignupRequestDto;
import com.solucitation.midpoint_backend.domain.member.dto.ValidationErrorResponse;
import com.solucitation.midpoint_backend.domain.member.exception.EmailAlreadyInUseException;
import com.solucitation.midpoint_backend.domain.member.exception.EmailNotVerifiedException;
import com.solucitation.midpoint_backend.domain.member.exception.NicknameAlreadyInUseException;
import com.solucitation.midpoint_backend.domain.member.exception.PasswordMismatchException;
import com.solucitation.midpoint_backend.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MemberController {
    private final MemberService memberService;

    /**
     * 새로운 회원을 등록합니다.
     *
     * @param signupRequestDto 회원가입 요청 DTO
     * @return 성공 시 200 OK와 성공 메시지를 반환, 실패 시 400 Bad Request와 오류 메시지를 반환
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUpMember(@Valid @RequestBody SignupRequestDto signupRequestDto) {
        memberService.signUpMember(signupRequestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", "회원가입에 성공하였습니다!"));
    }

    /**
     * 회원가입 중 발생한 사용자 정의 예외를 처리합니다.
     *
     * @param e 발생한 사용자 정의 예외
     * @return 400 Bad Request와 구조화된 오류 메시지를 반환
     */
    @ExceptionHandler({EmailNotVerifiedException.class, NicknameAlreadyInUseException.class, EmailAlreadyInUseException.class, PasswordMismatchException.class})
    public ResponseEntity<ValidationErrorResponse> handleSignUpExceptions(RuntimeException e) {
        String field;
        if (e instanceof EmailNotVerifiedException) {
            field = "email";
        } else if (e instanceof NicknameAlreadyInUseException) {
            field = "nickname";
        } else if (e instanceof EmailAlreadyInUseException) {
            field = "email";
        } else if (e instanceof PasswordMismatchException) {
            field = "password";
        } else {
            field = "unknown";
        }

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                Collections.singletonList(new ValidationErrorResponse.FieldError(field, e.getMessage()))
        );
        log.error("회원가입 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * DTO 검증 실패 예외를 처리합니다.
     *
     * @param e MethodArgumentNotValidException 예외
     * @return 400 Bad Request와 구조화된 검증 오류 메시지를 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(errors);
        log.error("회원가입 검증 실패: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 잘못된 요청 예외를 처리합니다.
     *
     * @param e IllegalArgumentException 예외
     * @return 400 Bad Request와 구조화된 오류 메시지를 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ValidationErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                Collections.singletonList(new ValidationErrorResponse.FieldError("IllegalArgumentException", e.getMessage()))
        );
        log.error("잘못된 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}