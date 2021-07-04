package com.garyclayburg.memuser

import com.unboundid.scim2.common.exceptions.ScimException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * <br><br>
 * Created 2021-07-04 09:18
 *
 * @author Gary Clayburg
 */
@ControllerAdvice
class CustomExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    //Tell spring web to use SCIM error information instead of just throwing a generic 500
    // error if a SCimException is thrown from a REST endpoint
    @ExceptionHandler(ScimException.class)
    ResponseEntity handleException(ScimException se) {
        return ResponseEntity.status(se.getScimError().getStatus()).body(se.getScimError())
    }
}
