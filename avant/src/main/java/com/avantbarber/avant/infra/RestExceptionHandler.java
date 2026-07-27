package com.avantbarber.avant.infra;

import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.HorarioFuncionamentoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.avantbarber.avant.exception.ChaveDuplicadaException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler{
      
    @ExceptionHandler(ChaveDuplicadaException.class)
    private ResponseEntity<RestErrorMessage> chaveDuplicadaHandler(ChaveDuplicadaException exception,  HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new RestErrorMessage(
            java.time.LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Registro duplicado",
            exception.getMessage(),
            request.getRequestURI()
        ));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    private ResponseEntity<RestErrorMessage> recursoNaoEncontradoHandler(RecursoNaoEncontradoException exception, HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RestErrorMessage(
            java.time.LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Recurso não encontrado",
            exception.getMessage(),
            request.getRequestURI()
        ));
    }

    @ExceptionHandler(HorarioFuncionamentoException.class)
    private ResponseEntity<RestErrorMessage> horarioFuncionamentoHandler(HorarioFuncionamentoException exception, HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestErrorMessage(
            java.time.LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Horário de funcionamento inválido",
            exception.getMessage(),
            request.getRequestURI()
        ));
    }

    @ExceptionHandler(BusinessException.class)
    private ResponseEntity<RestErrorMessage> businessExceptionHandler(BusinessException exception, HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestErrorMessage(
            java.time.LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Regra de negócio violada",
            exception.getMessage(),
            request.getRequestURI()
        ));
    }

}