# EduLearn LMS: Exception Handling Deep-Dive

This guide explains how your system handles errors and communicates them to the frontend without crashing.

---

## 1. The Global Strategy: @ControllerAdvice
**Definition**: In Spring Boot, `@ControllerAdvice` is an annotation that allows you to handle exceptions across the whole application in one single, global component.

### Line-by-Line Breakdown: `GlobalExceptionHandler.java`
```java
@ControllerAdvice // 1. The Global "Safety Net"
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class) // 2. Catch Validation Errors
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage); // 3. Collect all field errors
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // 4. Return 400 Bad Request
    }
    
    @ExceptionHandler(RuntimeException.class) // 5. Catch Business Logic Errors
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
```

### 🏫 Teaching the "Magic":
*   **Line 8 (`@ExceptionHandler`)**: This method only triggers if a **Validation** error happens (like a missing email).
*   **Line 17 (`ResponseEntity`)**: We wrap the error message in a standard HTTP response. This ensures the frontend gets a clean JSON body instead of a scary 500 Error Page.
*   **Line 27 (`RuntimeException`)**: This is the "General Catch." It handles all the `throw new RuntimeException` calls we make in our Service layer.

---

## 2. Throwing Exceptions (The Service Layer)

In your `AuthServiceImpl.java`, we use exceptions to stop invalid operations.

### Code Example:
```java
if (userRepository.existsByEmail(request.getEmail())) {
    throw new RuntimeException("User with this email already exists!");
}
```
*   **The Flow**: 
    1. The Service detects an error (Duplicate Email).
    2. It "throws" the exception.
    3. The execution of the `register` method **stops immediately**.
    4. The **GlobalExceptionHandler** "catches" it.
    5. The user receives: `{"message": "User with this email already exists!"}`.

---

## 3. Validation Exceptions (@Valid)

### How it works:
1. When the Controller receives a request with `@Valid`, Spring checks the DTO annotations (like `@Email`).
2. If the data is bad, Spring throws a `MethodArgumentNotValidException`.
3. The **GlobalExceptionHandler** catches this specific class and extracts the field-level messages you wrote in your DTO (e.g., "Email is required").

---

## 4. Key Takeaways for the Meeting

1.  **Consistency**: No matter which service an error happens in, the user always gets a clean, predictable JSON response.
2.  **Security**: By catching exceptions globally, we prevent the "Stack Trace" (internal code details) from being shown to the user or hackers.
3.  **Clean Code**: Our controllers don't have messy `try-catch` blocks. The "Happy Path" stays clean, and errors are handled elsewhere.

---

### Summary Checklist for the Architect:
*   **"How do you handle validation errors?"** ➔ We catch `MethodArgumentNotValidException` globally and return a map of field errors.
*   **"How do you communicate service failures?"** ➔ We throw `RuntimeException` in the Service layer, which is caught and turned into a JSON response by our `@ControllerAdvice`.
*   **"What happens if an unexpected error occurs?"** ➔ Our "General Catch" for `RuntimeException` ensures the user gets a readable message instead of a system crash.
