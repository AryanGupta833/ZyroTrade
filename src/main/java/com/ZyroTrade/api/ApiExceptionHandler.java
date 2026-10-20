package com.ZyroTrade.api;

import com.ZyroTrade.Orders.IdempotencyConflictException;
import com.ZyroTrade.Orders.InsufficientHoldingsException;
import com.ZyroTrade.Orders.CostBasisUnavailableException;
import com.ZyroTrade.Orders.QuoteUnavailableException;
import com.ZyroTrade.Orders.TradingHaltedException;
import com.ZyroTrade.Price.MarketDataUnavailableException;
import com.ZyroTrade.ledger.LedgerCutoverRequiredException;
import com.ZyroTrade.Wallet.InsufficientFundsException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.AuthenticationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InsufficientFundsException.class)
    ResponseEntity<ApiError> insufficientFunds(InsufficientFundsException exception) {
        return response(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", exception.getMessage());
    }

    @ExceptionHandler(InsufficientHoldingsException.class)
    ResponseEntity<ApiError> insufficientHoldings(InsufficientHoldingsException exception) {
        return response(HttpStatus.CONFLICT, "INSUFFICIENT_HOLDINGS", exception.getMessage());
    }

    @ExceptionHandler(CostBasisUnavailableException.class)
    ResponseEntity<ApiError> costBasisUnavailable(CostBasisUnavailableException exception) {
        return response(HttpStatus.CONFLICT, "COST_BASIS_REVIEW_REQUIRED", exception.getMessage());
    }

    @ExceptionHandler(QuoteUnavailableException.class)
    ResponseEntity<ApiError> quoteUnavailable(QuoteUnavailableException exception) {
        return response(HttpStatus.CONFLICT, "QUOTE_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(TradingHaltedException.class)
    ResponseEntity<ApiError> tradingHalted(TradingHaltedException exception) {
        return response(HttpStatus.CONFLICT, "TRADING_HALTED", exception.getMessage());
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    ResponseEntity<ApiError> marketDataUnavailable(MarketDataUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(LedgerCutoverRequiredException.class)
    ResponseEntity<ApiError> ledgerCutoverRequired(LedgerCutoverRequiredException exception) {
        return response(HttpStatus.CONFLICT, "LEDGER_CUTOVER_REQUIRED", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authenticationFailure(AuthenticationException exception) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username or password is invalid");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> idempotencyConflict(IdempotencyConflictException exception) {
        return response(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(
                error.getField(), error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(java.time.Instant.now(), 400,
                "VALIDATION_ERROR", "Request validation failed", fields));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request parameters or body are invalid");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> statusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String safeMessage = status.is4xxClientError() ? exception.getReason() : "Request could not be completed";
        return response(status, status.name(), safeMessage == null ? status.getReasonPhrase() : safeMessage);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed");
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code,
                message == null || message.isBlank() ? status.getReasonPhrase() : message));
    }
}
