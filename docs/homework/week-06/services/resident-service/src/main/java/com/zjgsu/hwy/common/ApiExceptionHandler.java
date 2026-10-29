package com.zjgsu.hwy.common;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResourceNotFoundException.class)
 public ResponseEntity<Map<String,String>> notFound(ResourceNotFoundException ex){
  return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",ex.getMessage()));
 }
 @ExceptionHandler(IllegalArgumentException.class)
 public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex){
  return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex){
  String msg=ex.getBindingResult().getFieldErrors().stream()
   .map(e->e.getField()+": "+e.getDefaultMessage()).collect(Collectors.joining("; "));
  return ResponseEntity.badRequest().body(Map.of("error",msg));
 }
}
