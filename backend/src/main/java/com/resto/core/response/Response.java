package com.resto.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private HttpStatus status;
    private int statusCode;
    private String message;
    private String service;
    private T data;

    public static <T> ResponseBuilder<T> builder() {
        return new ResponseBuilder<T>().service("RESTO-OS");
    }
}
