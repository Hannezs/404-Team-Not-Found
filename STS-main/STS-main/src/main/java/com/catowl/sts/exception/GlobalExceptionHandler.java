package com.catowl.sts.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequestException(BadRequestException ex) {
        Response response=new Response(HttpStatus.BAD_REQUEST.value(),ex.getMessage());
        logger.error("请求异常：",ex);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InternetServerException.class)
    public ResponseEntity<?> handleInternetServerException(InternetServerException ex) {
        Response response=new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage());
        logger.error("服务器异常：",ex);
        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
        Response response=new Response(HttpStatus.UNAUTHORIZED.value(),ex.getMessage());
        logger.error("身份认证异常：",ex);
        return new ResponseEntity<>(response,HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleRestException(Exception ex){
        Response response=new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage());
        logger.error("未知错误：",ex);
        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
    }
}