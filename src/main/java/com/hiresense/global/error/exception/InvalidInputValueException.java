package com.hiresense.global.error.exception;

import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;

public class InvalidInputValueException extends BusinessException {

    public InvalidInputValueException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}
