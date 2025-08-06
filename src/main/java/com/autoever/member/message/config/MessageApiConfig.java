package com.autoever.member.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 메시지 API 설정 클래스
 */
@Component
@ConfigurationProperties(prefix = "message.api")
public class MessageApiConfig {
    
    private KakaoTalkConfig kakaotalk = new KakaoTalkConfig();
    private SmsConfig sms = new SmsConfig();
    
    public KakaoTalkConfig getKakaotalk() {
        return kakaotalk;
    }
    
    public void setKakaotalk(KakaoTalkConfig kakaotalk) {
        this.kakaotalk = kakaotalk;
    }
    
    public SmsConfig getSms() {
        return sms;
    }
    
    public void setSms(SmsConfig sms) {
        this.sms = sms;
    }
    
    /**
     * 카카오톡 API 설정
     */
    public static class KakaoTalkConfig {
        private String baseUrl = "http://localhost:8081";
        private String username = "autoever";
        private String password = "1234";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        
        // getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
    
    /**
     * SMS API 설정
     */
    public static class SmsConfig {
        private String baseUrl = "http://localhost:8082";
        private String username = "autoever";
        private String password = "5678";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        
        // getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
}