package com.chess.chessverse.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleAny(Exception ex){
        Map<String,Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getClass().getSimpleName()+": "+ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex){
        Map<String,Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "ValidationError");
        return ResponseEntity.badRequest().body(body);
    }
}
