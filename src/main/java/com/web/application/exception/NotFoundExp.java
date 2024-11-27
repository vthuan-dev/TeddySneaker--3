package com.web.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundExp extends RuntimeException{

    public NotFoundExp(String message){
        super(message);
    }
}
