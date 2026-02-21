/**
 * 애플리케이션 전체에서 발생하는 예외를 한곳에서 처리하는 전역 예외 핸들러입니다.
 * 모든 에러 응답을 ApiResponse 규격으로 통일하여 클라이언트에 반환합니다.
 */
package com.maplog.common.exception;

import com.maplog.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 중 발생하는 커스텀 예외를 처리합니다.
     * (예: 중복 이메일, 존재하지 않는 데이터 등)
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    /**
     * @Valid 어노테이션을 통한 입력값 검증 실패 시 발생하는 예외를 처리합니다.
     * 폼 데이터나 쿼리 파라미터 바인딩 실패 시 호출됩니다.
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getAllErrors().isEmpty() 
                ? ErrorCode.BAD_REQUEST.getMessage() 
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        
        log.error("BindException: {}", message);
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * @RequestBody 모델 바인딩 시 @Valid 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().isEmpty() 
                ? ErrorCode.BAD_REQUEST.getMessage() 
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.error("MethodArgumentNotValidException: {}", message);
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * 예측하지 못한 서버 내부 시스템 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
