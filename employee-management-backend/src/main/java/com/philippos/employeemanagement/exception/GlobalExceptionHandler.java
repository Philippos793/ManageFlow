package com.philippos.employeemanagement.exception;

import com.philippos.employeemanagement.dto.response.ApiErrorResponse;
import com.philippos.employeemanagement.dto.response.EmployeeReactivationConflictResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException exception) {

        Map<String, String> validationErrors = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> validationErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                validationErrors
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidationException(
            HandlerMethodValidationException exception) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed");
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(
            BadCredentialsException exception) {

        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(EmployeeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeConflictException(
            EmployeeConflictException exception) {

        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InactiveEmployeeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveEmployeeConflictException(
            InactiveEmployeeConflictException exception) {
        HttpStatus status = HttpStatus.CONFLICT;
        return new ResponseEntity<>(new EmployeeReactivationConflictResponse(
                status.value(), status.getReasonPhrase(), exception.getMessage(),
                exception.getEmployeeId()), status);
    }
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeNotFoundException(
            EmployeeNotFoundException exception) {

        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDepartmentNotFoundException(
            DepartmentNotFoundException exception) {

        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DepartmentConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDepartmentConflictException(
            DepartmentConflictException exception) {

        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }
    @ExceptionHandler(RegistrationRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRegistrationRequestNotFoundException(
            RegistrationRequestNotFoundException exception) {

        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(RegistrationRequestConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleRegistrationRequestConflictException(
            RegistrationRequestConflictException exception) {

        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EmployeeAccountConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeAccountConflictException(
            EmployeeAccountConflictException exception) {

        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(WorkShiftConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkShiftConflictException(
            WorkShiftConflictException exception) {

        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception) {

        LOGGER.error("Unexpected server error", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    @ExceptionHandler(EmployeeInvitationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeInvitationNotFoundException(
            EmployeeInvitationNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EmployeeInvitationConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeInvitationConflictException(
            EmployeeInvitationConflictException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTaskNotFoundException(
            TaskNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(TaskConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleTaskConflictException(
            TaskConflictException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(TaskAttachmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTaskAttachmentNotFoundException(
            TaskAttachmentNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({TaskAttachmentValidationException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiErrorResponse> handleTaskAttachmentValidationException(Exception exception) {
        String message = exception instanceof TaskAttachmentValidationException
                ? exception.getMessage()
                : "Attachment exceeds the maximum allowed file size";
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingMultipartPart(
            MissingServletRequestPartException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Attachment file is required");
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotificationNotFoundException(
            NotificationNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message) {

        return buildResponse(status, message, null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            Map<String, String> validationErrors) {

        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                validationErrors
        );

        return new ResponseEntity<>(response, status);
    }
}

