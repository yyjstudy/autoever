package com.autoever.member.message.service;

import com.autoever.member.entity.User;
import com.autoever.member.message.dto.AgeRange;
import com.autoever.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

/**
 * 사용자 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {
    
    private final UserRepository userRepository;
    
    // 기본 배치 크기
    private static final int DEFAULT_BATCH_SIZE = 1000;
    
    /**
     * 연령대별 사용자 수 조회
     */
    public long countUsersByAgeRange(AgeRange ageRange) {
        long count = userRepository.countUsersByAgeRange(ageRange.getMinAge(), ageRange.getMaxAge());
        log.info("연령대 {} 사용자 수: {}", ageRange, count);
        return count;
    }
    
    /**
     * 연령대별 사용자 페이지 조회
     */
    public Page<User> getUsersByAgeRange(AgeRange ageRange, Pageable pageable) {
        return userRepository.findUsersByAgeRange(ageRange.getMinAge(), ageRange.getMaxAge(), pageable);
    }
    
    /**
     * 연령대별 사용자를 배치 단위로 처리
     * 
     * @param ageRange 연령 범위
     * @param batchProcessor 각 배치를 처리할 함수
     */
    public void processUsersByAgeRangeInBatches(AgeRange ageRange, Consumer<List<User>> batchProcessor) {
        processUsersByAgeRangeInBatches(ageRange, DEFAULT_BATCH_SIZE, batchProcessor);
    }
    
    /**
     * 연령대별 사용자를 배치 단위로 처리 (배치 크기 지정)
     * 
     * @param ageRange 연령 범위
     * @param batchSize 배치 크기
     * @param batchProcessor 각 배치를 처리할 함수
     */
    public void processUsersByAgeRangeInBatches(AgeRange ageRange, int batchSize, Consumer<List<User>> batchProcessor) {
        int pageNumber = 0;
        Page<User> page;
        
        log.info("연령대 {} 사용자 배치 처리 시작 - 배치 크기: {}", ageRange, batchSize);
        
        do {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            page = getUsersByAgeRange(ageRange, pageable);
            
            if (!page.isEmpty()) {
                log.debug("페이지 {} 처리 중 - 사용자 수: {}", pageNumber, page.getNumberOfElements());
                batchProcessor.accept(page.getContent());
            }
            
            pageNumber++;
        } while (page.hasNext());
        
        log.info("연령대 {} 사용자 배치 처리 완료 - 총 페이지: {}, 총 사용자: {}", 
                ageRange, pageNumber, page.getTotalElements());
    }
    
    /**
     * 사용자 리스트를 더 작은 배치로 분할
     * 
     * @param users 사용자 리스트
     * @param subBatchSize 서브 배치 크기
     * @param subBatchProcessor 서브 배치 처리 함수
     */
    public void processInSubBatches(List<User> users, int subBatchSize, Consumer<List<User>> subBatchProcessor) {
        for (int i = 0; i < users.size(); i += subBatchSize) {
            int end = Math.min(i + subBatchSize, users.size());
            List<User> subBatch = users.subList(i, end);
            subBatchProcessor.accept(subBatch);
        }
    }
}