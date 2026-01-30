package com.example.postman.exceptionHandler;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for authentication and request exceptions.
 */
@ControllerAdvice
public class ExceptionHandler {

    /**
     * Handles username not found exceptions.
     *
     * @param exception the exception
     * @return bad request response
     */

    @org.springframework.web.bind.annotation.ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFoundException(UsernameNotFoundException exception) {
        return ResponseEntity.badRequest().body("bad credentials");
    }

    /**
     * Handles bad request exceptions.
     *
     * @param ex      the exception
     * @param request the web request
     * @return bad request response
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return ResponseEntity.badRequest().body("Ошибка: некорректный запрос.");
    }

    /**
     * Handles bad credentials exceptions.
     *
     * @param ex      the exception
     * @param request the web request
     * @return conflict response with error message
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }
}
