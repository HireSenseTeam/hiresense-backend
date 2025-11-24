package com.hiresense.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;

@Slf4j
@Component
public class BedrockErrorHandler {

    public <T> T handleError(Exception e, String operation, T defaultValue) {
        if (e instanceof SdkClientException) {
            SdkClientException sdkException = (SdkClientException) e;
            
            if (sdkException.getMessage() != null && sdkException.getMessage().contains("Unable to load credentials")) {
                log.error("[BedrockErrorHandler] {} - AWS 자격 증명을 찾을 수 없습니다.", operation);
                log.error("[BedrockErrorHandler] 환경 변수 설정: export AWS_ACCESS_KEY_ID=your-key && export AWS_SECRET_ACCESS_KEY=your-secret");
                log.error("[BedrockErrorHandler] 또는 ~/.aws/credentials 파일에 자격 증명을 설정하세요.");
            } else {
                log.error("[BedrockErrorHandler] {} - AWS SDK 클라이언트 오류: {}", operation, sdkException.getMessage(), sdkException);
            }
        } else if (e instanceof SdkServiceException) {
            SdkServiceException serviceException = (SdkServiceException) e;
            log.error("[BedrockErrorHandler] {} - AWS 서비스 오류: {} (상태 코드: {})", 
                    operation, serviceException.getMessage(), serviceException.statusCode(), serviceException);
        } else {
            log.error("[BedrockErrorHandler] {} - 예상치 못한 오류: {}", operation, e.getMessage(), e);
        }
        
        return defaultValue;
    }

    public void handleErrorAndThrow(Exception e, String operation) {
        if (e instanceof SdkClientException) {
            SdkClientException sdkException = (SdkClientException) e;
            
            if (sdkException.getMessage() != null && sdkException.getMessage().contains("Unable to load credentials")) {
                log.error("[BedrockErrorHandler] {} - AWS 자격 증명을 찾을 수 없습니다.", operation);
                log.error("[BedrockErrorHandler] 환경 변수 설정: export AWS_ACCESS_KEY_ID=your-key && export AWS_SECRET_ACCESS_KEY=your-secret");
                log.error("[BedrockErrorHandler] 또는 ~/.aws/credentials 파일에 자격 증명을 설정하세요.");
            } else {
                log.error("[BedrockErrorHandler] {} - AWS SDK 클라이언트 오류: {}", operation, sdkException.getMessage(), sdkException);
            }
        } else if (e instanceof SdkServiceException) {
            SdkServiceException serviceException = (SdkServiceException) e;
            log.error("[BedrockErrorHandler] {} - AWS 서비스 오류: {} (상태 코드: {})", 
                    operation, serviceException.getMessage(), serviceException.statusCode(), serviceException);
        } else {
            log.error("[BedrockErrorHandler] {} - 예상치 못한 오류: {}", operation, e.getMessage(), e);
        }
        
        throw new RuntimeException(operation + " 실패: " + e.getMessage(), e);
    }
}
