package com.gps.tracking.exception

import com.gps.tracking.dto.respon.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant


@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_FOUND

        return ResponseEntity.status(status).body(
            ErrorResponse(
                Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message ?: "Resource not found",
                path = request.requestURI
            )
        )
    }


    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message ?: "Invalid request",
                path = request.requestURI
            )
        )
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex:MethodArgumentTypeMismatchException,
                           request: HttpServletRequest):ResponseEntity<ErrorResponse>{

        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(
            ErrorResponse(
                Instant.now(),
                status=status.value(),
                error = status.reasonPhrase,
                message = "Invalid value for parameter `${ex.name}`",
                path=request.requestURI
            )

        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidBody(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        return ResponseEntity.status(status).body(
            ErrorResponse(
                Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = "Invalid request body",
                path = request.requestURI
            )
        )
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.METHOD_NOT_ALLOWED

        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = "HTTP method '${ex.method}' is not supported for this endpoint",
                path = request.requestURI
            )
        )
    }


}