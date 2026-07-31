package com.eneik.generated.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FinancialAccessDeniedException extends RuntimeException {
    public FinancialAccessDeniedException(String message) {
        super(message);
    }
}
