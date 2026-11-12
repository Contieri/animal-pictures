package com.example.animalpictures;

import com.example.animalpictures.client.PictureFetchException;
import com.example.animalpictures.picture.InvalidAnimalTypeException;
import com.example.animalpictures.picture.PictureNotFoundException;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns every error as an RFC 9457 problem detail (JSON). Spring's base class covers the
 * standard MVC errors (bad parameter types, unknown routes...); the methods below add the domain errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidAnimalTypeException.class)
    ProblemDetail invalidAnimal(InvalidAnimalTypeException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid animal type", e.getMessage());
    }

    @ExceptionHandler(PictureNotFoundException.class)
    ProblemDetail notFound(PictureNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Picture not found", e.getMessage());
    }

    @ExceptionHandler(PictureFetchException.class)
    ProblemDetail fetchFailed(PictureFetchException e) {
        log.warn("Picture source failed: {}", e.getMessage(), e.getCause());
        return problem(HttpStatus.BAD_GATEWAY, "Picture source unavailable",
                e.getMessage() + ". Nothing was stored; please try again.");
    }

    /** Explains which parameter is invalid, e.g. "count: must be less than or equal to 10". */
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": "
                                + messageOf(error)))
                .collect(Collectors.joining("; "));
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String messageOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value";
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
