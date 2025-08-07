package com.autoever.member.message.template;

import com.autoever.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MessageTemplateService 테스트")
class MessageTemplateServiceTest {
    
    private MessageTemplateService templateService;
    
    @BeforeEach
    void setUp() {
        templateService = new MessageTemplateService();
    }
    
    @Test
    @DisplayName("User 엔티티를 사용한 템플릿 적용")
    void applyTemplate_WithUser_Success() {
        // given
        User user = User.builder()
            .username("testuser")
            .name("박민수")
            .socialNumber("900101-1234567")
            .phoneNumber("010-1234-5678")
            .email("test@example.com")
            .address("서울시 강남구")
            .build();
        String originalMessage = "회원님께 중요한 안내사항이 있습니다.";
        
        // when
        String result = templateService.applyTemplate(user, originalMessage);
        
        // then
        assertThat(result).startsWith("박민수님, 안녕하세요. 현대 오토에버입니다.");
        assertThat(result).contains(originalMessage);
    }
    
    @Test
    @DisplayName("회원 이름과 메시지로 템플릿 적용")
    void applyTemplate_WithNameAndMessage_Success() {
        // given
        String memberName = "최영희";
        String originalMessage = "이벤트에 당첨되셨습니다.";
        
        // when
        String result = templateService.applyTemplate(memberName, originalMessage);
        
        // then
        assertThat(result).isEqualTo("최영희님, 안녕하세요. 현대 오토에버입니다.\n\n이벤트에 당첨되셨습니다.");
    }
    
    @Test
    @DisplayName("null User로 템플릿 적용 시 예외 발생")
    void applyTemplate_NullUser_ThrowsException() {
        // given
        String originalMessage = "메시지 내용";
        
        // when & then
        assertThatThrownBy(() -> templateService.applyTemplate((User) null, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자 정보는 필수입니다.");
    }
    
    @Test
    @DisplayName("템플릿 적용 여부 확인 - 올바른 템플릿")
    void isTemplateApplied_ValidTemplate_ReturnsTrue() {
        // given
        String memberName = "김철수";
        String message = "김철수님, 안녕하세요. 현대 오토에버입니다.\n\n본문 내용";
        
        // when
        boolean result = templateService.isTemplateApplied(message, memberName);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("템플릿 적용 여부 확인 - 잘못된 템플릿")
    void isTemplateApplied_InvalidTemplate_ReturnsFalse() {
        // given
        String memberName = "김철수";
        String message = "안녕하세요. 본문 내용입니다.";
        
        // when
        boolean result = templateService.isTemplateApplied(message, memberName);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("템플릿 적용 여부 확인 - 다른 이름")
    void isTemplateApplied_DifferentName_ReturnsFalse() {
        // given
        String memberName = "김철수";
        String message = "이영희님, 안녕하세요. 현대 오토에버입니다.\n\n본문 내용";
        
        // when
        boolean result = templateService.isTemplateApplied(message, memberName);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("템플릿 적용 여부 확인 - null 메시지")
    void isTemplateApplied_NullMessage_ReturnsFalse() {
        // given
        String memberName = "김철수";
        
        // when
        boolean result = templateService.isTemplateApplied(null, memberName);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("템플릿 적용 여부 확인 - null 이름")
    void isTemplateApplied_NullName_ReturnsFalse() {
        // given
        String message = "김철수님, 안녕하세요. 현대 오토에버입니다.\n\n본문 내용";
        
        // when
        boolean result = templateService.isTemplateApplied(message, null);
        
        // then
        assertThat(result).isFalse();
    }
}