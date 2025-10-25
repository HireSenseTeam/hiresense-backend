package com.hiresense.global.error.exception;

import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;

public class JobPostingNotFoundException extends BusinessException {

    public JobPostingNotFoundException() {
        super(ErrorCode.JOB_POSTING_NOT_FOUND);
    }
}
