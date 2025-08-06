package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeRange;
import com.autoever.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService 테스트")
class UserQueryServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserQueryService userQueryService;
    
    private AgeRange twentiesRange;
    
    @BeforeEach
    void setUp() {
        twentiesRange = new AgeRange(20, 29);
    }
    
    @Test
    @DisplayName("연령대별 사용자 수 조회")
    void countUsersByAgeRange() {
        // Given
        when(userRepository.countUsersByAgeRange(20, 29)).thenReturn(15000L);
        
        // When
        long count = userQueryService.countUsersByAgeRange(twentiesRange);
        
        // Then
        assertThat(count).isEqualTo(15000L);
        verify(userRepository).countUsersByAgeRange(20, 29);
    }
    
    @Test
    @DisplayName("연령대별 사용자 페이지 조회")
    void getUsersByAgeRange() {
        // Given
        List<User> users = createTestUsers(10);
        Page<User> page = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(userRepository.findUsersByAgeRange(20, 29, pageable)).thenReturn(page);
        
        // When
        Page<User> result = userQueryService.getUsersByAgeRange(twentiesRange, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("배치 단위로 사용자 처리")
    void processUsersByAgeRangeInBatches() {
        // Given
        int batchSize = 10;
        AtomicInteger processedCount = new AtomicInteger(0);
        
        List<User> users = createTestUsers(10);
        Page<User> page = new PageImpl<>(users, PageRequest.of(0, batchSize), 10);
        
        when(userRepository.findUsersByAgeRange(eq(20), eq(29), any(Pageable.class)))
            .thenReturn(page);
        
        // When
        userQueryService.processUsersByAgeRangeInBatches(twentiesRange, batchSize, batch -> {
            processedCount.addAndGet(batch.size());
        });
        
        // Then
        assertThat(processedCount.get()).isEqualTo(10);
        verify(userRepository, atLeastOnce()).findUsersByAgeRange(eq(20), eq(29), any(Pageable.class));
    }
    
    @Test
    @DisplayName("서브 배치로 분할 처리")
    void processInSubBatches() {
        // Given
        List<User> users = createTestUsers(250);
        int subBatchSize = 100;
        AtomicInteger batchCount = new AtomicInteger(0);
        List<Integer> batchSizes = new ArrayList<>();
        
        // When
        userQueryService.processInSubBatches(users, subBatchSize, subBatch -> {
            batchCount.incrementAndGet();
            batchSizes.add(subBatch.size());
        });
        
        // Then
        assertThat(batchCount.get()).isEqualTo(3); // 100, 100, 50
        assertThat(batchSizes).containsExactly(100, 100, 50);
    }
    
    private List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = User.builder()
                .username("user" + i)
                .name("Test User " + i)
                .socialNumber("950101-1234567") // 29세
                .phoneNumber("010-1234-" + String.format("%04d", i))
                .email("user" + i + "@test.com")
                .password("password" + i)
                .address("서울시 강남구")
                .build();
            users.add(user);
        }
        return users;
    }
}