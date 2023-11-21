package com.arextest.schedule.beans;

import com.arextest.schedule.model.CommonResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ControllerException {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CommonResponse> handleValidException(MethodArgumentNotValidException e) {
    // setting application/json;charset=UTF-8
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

    List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
    String message = allErrors.stream().map(s -> s.getDefaultMessage())
        .collect(Collectors.joining(";"));

    return new ResponseEntity<>(CommonResponse.badResponse(message), headers,
        HttpStatus.BAD_REQUEST);
  }
}
