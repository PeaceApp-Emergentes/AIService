package com.upc.pre.peaceapp.ai.infrastructure.openai;

import org.springframework.http.HttpStatus;

public class OpenAiProviderException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public OpenAiProviderException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
