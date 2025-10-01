package com.hiresense.global.error.exception;

import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;

public class ResumeNotFoundException extends BusinessException {

    public ResumeNotFoundException() {
        super(ErrorCode.RESUME_NOT_FOUND);
    }
}
