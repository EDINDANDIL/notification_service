package com.example.postman.exceptions;


import lombok.Getter;

@Getter
public class InvalidJwt extends Exception{
    private final int code = 401;
}
