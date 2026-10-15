package com.example.githubsearcher.exception;

import org.springframework.http.HttpStatus;

/** Thrown when GitHub cannot be reached or returns something we cannot use. */
public class GithubApiException extends RuntimeException {

    private final HttpStatus status;

    public GithubApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public GithubApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
