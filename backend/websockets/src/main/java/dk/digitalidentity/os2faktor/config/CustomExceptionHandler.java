package dk.digitalidentity.os2faktor.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

	// we get some attempts to bombard our endpoint with crap payloads, and these logs ERROR by default, so those
	// are suppressed as warnings instead. Expand as needed
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ Exception.class })
    public void handleException(Exception ex) {
    	if (ex instanceof MultipartException) {
    		log.warn("suppressing MultiPartException: " + ex.getMessage());
    	}
    	else if (ex instanceof InvalidMediaTypeException) {
    		log.warn("suppressing InvalidMediaTypeException: " + ex.getMessage());
    	}
    	else if (ex instanceof HttpMediaTypeNotAcceptableException) {
    		log.warn("suppressing HttpMediaTypeNotAcceptableException: " + ex.getMessage());
    	}
    	else {
    		log.error("Exception caught from controller", ex);
    	}
    }

}
