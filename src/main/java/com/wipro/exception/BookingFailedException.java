package com.wipro.exception;

import com.wipro.dto.ErrorResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class BookingFailedException extends RuntimeException{

    private HttpStatus httpStatus;
    private ErrorResponse errorResponse;
}
