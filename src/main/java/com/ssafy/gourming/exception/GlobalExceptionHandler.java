package com.ssafy.gourming.exception;

import java.util.NoSuchElementException;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기 (Global Exception Handler)
 * 각 컨트롤러에서 발생하는 예외를 한 곳에서 처리하여 일관된 HTTP 응답 포맷을 유지합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 400 Bad Request: DTO 유효성 검사 실패 (@Valid)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
		// 첫 번째로 발생한 에러 메시지를 반환
		String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
		return ResponseEntity.badRequest().body(errorMessage);
	}

	/**
	 * 400 Bad Request: 쿼리 파라미터 등 컨트롤러 메서드 파라미터 유효성 검사 실패
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<?> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
		return ResponseEntity.badRequest().body("요청 파라미터가 올바르지 않습니다.");
	}

	/**
	 * 400 Bad Request: 잘못된 인자 또는 요청 (비즈니스 로직 유효성 검사 실패 등)
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	/**
	 * 401 Unauthorized: 인증 실패 (비밀번호 불일치 등)
	 */
	@ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
	public ResponseEntity<?> handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
	}

	/**
	 * 403 Forbidden: 접근 권한 없음 (타인 프로필 수정 등)
	 */
	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<?> handleSecurityException(SecurityException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
	}

	/**
	 * 403 Forbidden: 권한 부족 (Spring Security @PreAuthorize 등)
	 */
	@ExceptionHandler({
		org.springframework.security.authorization.AuthorizationDeniedException.class,
		org.springframework.security.access.AccessDeniedException.class
	})
	public ResponseEntity<?> handleAuthorizationDeniedException(Exception ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
	}

	/**
	 * 404 Not Found: 리소스를 찾을 수 없음 (존재하지 않는 유저 등)
	 */
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	/**
	 * 500 Internal Server Error: 기타 예기치 않은 서버 오류
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneralException(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
	}

	/**
	 * 400 Bad Request: Path Variable, Request Param 등의 검증 실패 (@Validated)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex) {
		return ResponseEntity.badRequest().body("요청 파라미터가 유효하지 않습니다.");
	}
}
