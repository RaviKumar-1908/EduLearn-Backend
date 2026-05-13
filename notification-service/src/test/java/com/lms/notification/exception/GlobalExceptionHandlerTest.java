package com.lms.notification.exception;

import com.lms.notification.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotificationNotFound_returns404() {
        var response = handler.handleNotificationNotFound(new NotificationNotFoundException(9));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatusCode());
    }

    @Test
    void handleValidationErrors_returnsFieldMap() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "title must not be blank"));
        bindingResult.addError(new FieldError("request", "message", "message must not be blank"));

        Method method = DummyController.class.getDeclaredMethod("create", Object.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Map<String, String>> body = response.getBody();
        assertFalse(body.isSuccess());
        assertEquals("title must not be blank", body.getData().get("title"));
        assertEquals("message must not be blank", body.getData().get("message"));
    }

    @Test
    void handleConstraintViolation_returns400() {
        var response = handler.handleConstraintViolation(new ConstraintViolationException("bad constraint", Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatusCode());
    }

    @Test
    void handleMalformedJson_returns400() {
        var response = handler.handleMalformedJson(new HttpMessageNotReadableException("bad json"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request body", response.getBody().getMessage());
    }

    @Test
    void handleTypeMismatch_returns400() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "userId", null, new IllegalArgumentException());

        var response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("userId"));
    }

    @Test
    void handleGeneric_returns500() {
        var response = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatusCode());
    }

    static class DummyController {
        public void create(Object request) {
        }
    }
}
