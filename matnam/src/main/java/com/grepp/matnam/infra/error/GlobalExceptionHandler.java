package com.grepp.matnam.infra.error;

import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.response.ApiResponse;
import com.grepp.matnam.infra.response.Messages;
import com.grepp.matnam.infra.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("유효하지 않은 인자 예외: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse(ResponseCode.BAD_REQUEST.code(), "요청 처리 실패", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("유효하지 않은 상태 예외: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse(ResponseCode.BAD_REQUEST.code(), "요청 처리 실패", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.error("유효성 검증 실패: {}", errors);
        return ResponseEntity.badRequest()
                .body(new ApiResponse(ResponseCode.BAD_REQUEST.code(), "입력값 검증 실패", errors));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.error("서버 오류 발생: {}", ex.getMessage());

        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            ApiResponse response = new ApiResponse("4004", "리소스를 찾을 수 없습니다.", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("error/404");
            modelAndView.addObject("errorMessage", "요청하신 페이지를 찾을 수 없습니다.");
            modelAndView.addObject("errorDetails", ex.getMessage());
            return modelAndView;
        }
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("페이지를 찾을 수 없음: {}", ex.getMessage());

        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            ApiResponse response = new ApiResponse("4004", "페이지를 찾을 수 없습니다.", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("error/404");
            modelAndView.addObject("errorMessage", "요청하신 페이지를 찾을 수 없습니다.");
            modelAndView.addObject("errorDetails", ex.getRequestURL());
            return modelAndView;
        }
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse> handleCommonException(CommonException e) {
        log.info("CommonException 발생: {}", e.getMessage());
        return ResponseEntity
            .status(e.code().status())
            .body(new ApiResponse(e.code().code(), e.getMessage(), null));
    }


    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        if (ex instanceof CommonException commonEx) {
            log.info("🔥 CommonException (from Exception.class) 처리됨: {}", commonEx.getMessage());
            return ResponseEntity
                .status(commonEx.code().status())
                .body(new ApiResponse(commonEx.code().code(), commonEx.getMessage(), null));
        }


        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            ApiResponse response = new ApiResponse("5000", "서버 오류가 발생했습니다.", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } else {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("error/500");
            modelAndView.addObject("errorMessage", "서버 오류가 발생했습니다.");
            modelAndView.addObject("errorDetails", ex.getMessage());
            return modelAndView;
        }
    }
}