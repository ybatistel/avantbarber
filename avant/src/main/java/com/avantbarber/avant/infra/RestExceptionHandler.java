package com.avantbarber.avant.infra;

import com.avantbarber.avant.exception.HorarioFuncionamentoException;
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
    private ResponseEntity<String> chaveDuplicadaHandler(ChaveDuplicadaException exception){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    private ResponseEntity<String> recursoNaoEncontradoHandler(RecursoNaoEncontradoException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(HorarioFuncionamentoException.class)
    private ResponseEntity<String> horarioFuncionamentoHandler(HorarioFuncionamentoException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    

}