package com.autoever.member.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 구조화된 메시지 로깅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredMessageLogger {
    
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    /**
     * 작업 시작 로그
     */
    public void logJobStart(UUID jobId, String ageGroup, String message, int totalUsers) {
        Map<String, Object> logData = createBaseLogData("JOB_START", jobId.toString());
        logData.put("ageGroup", ageGroup);
        logData.put("messageLength", message.length());
        logData.put("totalUsers", totalUsers);
        logData.put("estimatedDuration", calculateEstimatedDuration(totalUsers));
        
        logStructured("info", "대량 메시지 발송 작업 시작", logData);
    }
    
    /**
     * 배치 처리 로그
     */
    public void logBatchProcessing(UUID jobId, int batchNumber, int batchSize, long durationMs, int successCount, int failureCount) {
        Map<String, Object> logData = createBaseLogData("BATCH_PROCESSING", jobId.toString());
        logData.put("batchNumber", batchNumber);
        logData.put("batchSize", batchSize);
        logData.put("durationMs", durationMs);
        logData.put("successCount", successCount);
        logData.put("failureCount", failureCount);
        logData.put("successRate", successCount + failureCount > 0 ? (double) successCount / (successCount + failureCount) * 100 : 0);
        logData.put("throughputPerSecond", durationMs > 0 ? (double) batchSize / durationMs * 1000 : 0);
        
        logStructured("info", "배치 처리 완료", logData);
    }
    
    /**
     * 메시지 발송 실패 로그
     */
    public void logMessageFailure(UUID jobId, String phoneNumber, String errorMessage, long responseTimeMs) {
        Map<String, Object> logData = createBaseLogData("MESSAGE_FAILURE", jobId.toString());
        logData.put("phoneNumber", maskPhoneNumber(phoneNumber));
        logData.put("errorMessage", errorMessage);
        logData.put("responseTimeMs", responseTimeMs);
        
        logStructured("warn", "메시지 발송 실패", logData);
    }
    
    
    /**
     * 작업 완료 로그
     */
    public void logJobCompletion(UUID jobId, String status, int totalUsers, int successCount, int failureCount, long durationMs) {
        Map<String, Object> logData = createBaseLogData("JOB_COMPLETION", jobId.toString());
        logData.put("status", status);
        logData.put("totalUsers", totalUsers);
        logData.put("successCount", successCount);
        logData.put("failureCount", failureCount);
        logData.put("successRate", totalUsers > 0 ? (double) successCount / totalUsers * 100 : 0);
        logData.put("durationMs", durationMs);
        logData.put("averageTimePerMessage", totalUsers > 0 ? (double) durationMs / totalUsers : 0);
        
        String level = failureCount > successCount ? "warn" : "info";
        logStructured(level, "대량 메시지 발송 작업 완료", logData);
    }
    
    /**
     * 시스템 리소스 상태 로그
     */
    public void logSystemResources(BatchProcessingService.ThreadPoolStatus threadPoolStatus, int currentBatchSize) {
        Map<String, Object> logData = createBaseLogData("SYSTEM_RESOURCES", null);
        logData.put("threadPool", Map.of(
            "corePoolSize", threadPoolStatus.corePoolSize(),
            "maxPoolSize", threadPoolStatus.maxPoolSize(),
            "activeCount", threadPoolStatus.activeCount(),
            "poolSize", threadPoolStatus.poolSize(),
            "queueSize", threadPoolStatus.queueSize(),
            "queueCapacity", threadPoolStatus.queueCapacity(),
            "completedTaskCount", threadPoolStatus.completedTaskCount(),
            "utilization", threadPoolStatus.getUtilization(),
            "queueUtilization", threadPoolStatus.getQueueUtilization()
        ));
        logData.put("currentBatchSize", currentBatchSize);
        logData.put("memoryUsage", getMemoryUsage());
        
        logStructured("debug", "시스템 리소스 상태", logData);
    }
    
    
    /**
     * 에러 상황 로그
     */
    public void logError(UUID jobId, String operation, Exception error) {
        Map<String, Object> logData = createBaseLogData("ERROR", jobId != null ? jobId.toString() : null);
        logData.put("operation", operation);
        logData.put("errorClass", error.getClass().getSimpleName());
        logData.put("errorMessage", error.getMessage());
        logData.put("stackTrace", getStackTraceAsString(error));
        
        logStructured("error", "작업 중 오류 발생", logData);
    }
    
    /**
     * 기본 로그 데이터 생성
     */
    private Map<String, Object> createBaseLogData(String eventType, String jobId) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        logData.put("eventType", eventType);
        logData.put("service", "BulkMessageService");
        if (jobId != null) {
            logData.put("jobId", jobId);
        }
        return logData;
    }
    
    /**
     * 구조화된 로그 출력
     */
    private void logStructured(String level, String message, Map<String, Object> data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            
            // MDC에 jobId 설정 (로그 추적용)
            if (data.containsKey("jobId")) {
                MDC.put("jobId", data.get("jobId").toString());
            }
            
            switch (level.toLowerCase()) {
                case "debug" -> log.debug("{} | {}", message, jsonData);
                case "info" -> log.info("{} | {}", message, jsonData);
                case "warn" -> log.warn("{} | {}", message, jsonData);
                case "error" -> log.error("{} | {}", message, jsonData);
                default -> log.info("{} | {}", message, jsonData);
            }
        } catch (JsonProcessingException e) {
            log.error("구조화된 로그 생성 실패: {}", e.getMessage());
            log.info(message); // 폴백으로 일반 로그 출력
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    /**
     * 예상 소요 시간 계산
     */
    private String calculateEstimatedDuration(int totalUsers) {
        // 사용자 1000명당 약 1분으로 가정
        long estimatedMinutes = totalUsers / 1000 + 1;
        if (estimatedMinutes < 60) {
            return estimatedMinutes + "분";
        } else {
            long hours = estimatedMinutes / 60;
            long minutes = estimatedMinutes % 60;
            return hours + "시간 " + minutes + "분";
        }
    }
    
    /**
     * 메모리 사용량 조회
     */
    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return Map.of(
            "totalMemoryMB", totalMemory / 1024 / 1024,
            "usedMemoryMB", usedMemory / 1024 / 1024,
            "freeMemoryMB", freeMemory / 1024 / 1024,
            "maxMemoryMB", maxMemory / 1024 / 1024,
            "memoryUtilization", (double) usedMemory / maxMemory * 100
        );
    }
    
    /**
     * 스택 트레이스를 문자열로 변환 (처음 5줄만)
     */
    private String getStackTraceAsString(Exception error) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = error.getStackTrace();
        int limit = Math.min(5, stackTrace.length);
        
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(stackTrace[i].toString());
        }
        
        return sb.toString();
    }
}