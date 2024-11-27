package com.web.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerExp extends RuntimeException{
    public InternalServerExp(String message){
        super(message);
    }
}
