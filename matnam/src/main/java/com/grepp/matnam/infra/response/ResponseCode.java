package com.grepp.matnam.infra.response;

import org.springframework.http.HttpStatus;

public enum ResponseCode {
    OK("0000", HttpStatus.OK, "정상적으로 완료되었습니다."),
    BAD_REQUEST("4000", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("4001", HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    INTERNAL_SERVER_ERROR("5000", HttpStatus.INTERNAL_SERVER_ERROR, "서버에러 입니다."),
    NOT_FOUND("4004", HttpStatus.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    CONFLICT("4009", HttpStatus.CONFLICT, "중복된 데이터입니다."),
    INVALID_COUPON("4010", HttpStatus.BAD_REQUEST, "유효하지 않은 쿠폰입니다.");
    
    private final String code;
    private final HttpStatus status;
    private final String message;
    
    ResponseCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
    
    public String code() {
        return code;
    }
    
    public HttpStatus status() {
        return status;
    }
    
    public String message() {
        return message;
    }
}
