package com.autoever.member.message.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MessageTemplate 테스트")
class MessageTemplateTest {
    
    @Test
    @DisplayName("정상적인 템플릿 적용")
    void applyTemplate_Success() {
        // given
        String memberName = "홍길동";
        String originalMessage = "새로운 이벤트가 시작되었습니다.";
        
        // when
        String result = MessageTemplate.applyTemplate(memberName, originalMessage);
        
        // then
        assertThat(result).isEqualTo("홍길동님, 안녕하세요. 현대 오토에버입니다.\n\n새로운 이벤트가 시작되었습니다.");
    }
    
    @Test
    @DisplayName("회원 이름에 공백이 있는 경우")
    void applyTemplate_WithSpacesInName() {
        // given
        String memberName = "  김철수  ";
        String originalMessage = "메시지 내용";
        
        // when
        String result = MessageTemplate.applyTemplate(memberName, originalMessage);
        
        // then
        assertThat(result).startsWith("김철수님, 안녕하세요.");
    }
    
    @Test
    @DisplayName("null 회원 이름으로 템플릿 적용 시 예외 발생")
    void applyTemplate_NullMemberName_ThrowsException() {
        // given
        String originalMessage = "메시지 내용";
        
        // when & then
        assertThatThrownBy(() -> MessageTemplate.applyTemplate(null, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 이름은 필수입니다.");
    }
    
    @Test
    @DisplayName("빈 회원 이름으로 템플릿 적용 시 예외 발생")
    void applyTemplate_EmptyMemberName_ThrowsException() {
        // given
        String memberName = "   ";
        String originalMessage = "메시지 내용";
        
        // when & then
        assertThatThrownBy(() -> MessageTemplate.applyTemplate(memberName, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 이름은 필수입니다.");
    }
    
    @Test
    @DisplayName("null 메시지로 템플릿 적용 시 예외 발생")
    void applyTemplate_NullMessage_ThrowsException() {
        // given
        String memberName = "홍길동";
        
        // when & then
        assertThatThrownBy(() -> MessageTemplate.applyTemplate(memberName, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("메시지 내용은 필수입니다.");
    }
    
    @Test
    @DisplayName("빈 메시지로 템플릿 적용 시 예외 발생")
    void applyTemplate_EmptyMessage_ThrowsException() {
        // given
        String memberName = "홍길동";
        String originalMessage = "   ";
        
        // when & then
        assertThatThrownBy(() -> MessageTemplate.applyTemplate(memberName, originalMessage))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("메시지 내용은 필수입니다.");
    }
    
    @Test
    @DisplayName("인사말만 생성")
    void createGreeting_Success() {
        // given
        String memberName = "이영희";
        
        // when
        String greeting = MessageTemplate.createGreeting(memberName);
        
        // then
        assertThat(greeting).isEqualTo("이영희님, 안녕하세요. 현대 오토에버입니다.");
    }
    
    @Test
    @DisplayName("null 이름으로 인사말 생성 시 예외 발생")
    void createGreeting_NullName_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> MessageTemplate.createGreeting(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 이름은 필수입니다.");
    }
}