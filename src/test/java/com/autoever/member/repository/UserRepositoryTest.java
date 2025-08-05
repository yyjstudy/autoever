package com.autoever.member.repository;

import com.autoever.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .username("testuser1")
                .password("password123!")
                .name("홍길동")
                .socialNumber("901201-1234567")
                .phoneNumber("010-1234-5678")
                .address("서울시 강남구")
                .build();

        testUser2 = User.builder()
                .username("testuser2")
                .password("password123!")
                .name("김철수")
                .socialNumber("890101-1234567")
                .phoneNumber("010-9876-5432")
                .address("부산시 해운대구")
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 테스트")
    void findByUsernameTest() {
        // when
        Optional<User> foundUser = userRepository.findByUsername("testuser1");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
        assertThat(foundUser.get().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 - 존재하지 않는 경우")
    void findByUsernameNotFoundTest() {
        // when
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인 테스트")
    void existsByUsernameTest() {
        // when & then
        assertThat(userRepository.existsByUsername("testuser1")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("주민번호 존재 여부 확인 테스트")
    void existsBySocialNumberTest() {
        // when & then
        assertThat(userRepository.existsBySocialNumber("901201-1234567")).isTrue();
        assertThat(userRepository.existsBySocialNumber("999999-9999999")).isFalse();
    }

    @Test
    @DisplayName("사용자명 포함 검색 테스트")
    void findByUsernameContainingTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByUsernameContaining("testuser", pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("이름 포함 검색 테스트")
    void findByNameContainingTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByNameContaining("길동", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자명과 이름 모두 포함 검색 테스트")
    void findByUsernameContainingAndNameContainingTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByUsernameContainingAndNameContaining("testuser1", "홍", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("홍길동");
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("특정 시간 이후 생성된 사용자 수 조회 테스트")
    void countByCreatedAtGreaterThanEqualTest() {
        // given
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        // when
        long count = userRepository.countByCreatedAtGreaterThanEqual(startOfToday);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("선택적 필터를 사용한 사용자 검색 테스트")
    void findUsersWithOptionalFiltersTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when - 사용자명만 필터
        Page<User> result1 = userRepository.findUsersWithOptionalFilters("testuser1", null, pageable);
        
        // then
        assertThat(result1.getContent()).hasSize(1);
        assertThat(result1.getContent().get(0).getUsername()).isEqualTo("testuser1");

        // when - 이름만 필터
        Page<User> result2 = userRepository.findUsersWithOptionalFilters(null, "김철수", pageable);
        
        // then
        assertThat(result2.getContent()).hasSize(1);
        assertThat(result2.getContent().get(0).getName()).isEqualTo("김철수");

        // when - 모든 필터 null
        Page<User> result3 = userRepository.findUsersWithOptionalFilters(null, null, pageable);
        
        // then
        assertThat(result3.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("사용자 저장 및 UNIQUE 제약조건 테스트")
    void saveUserWithUniqueConstraintTest() {
        // given
        User duplicateUsernameUser = User.builder()
                .username("testuser1") // 이미 존재하는 사용자명
                .password("password123!")
                .name("다른사람")
                .socialNumber("950505-1234567")
                .phoneNumber("010-5555-5555")
                .address("대구시")
                .build();

        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUsernameUser);
            userRepository.flush(); // 즉시 DB에 반영하여 제약조건 확인
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("페이징 테스트")
    void pagingTest() {
        // given
        for (int i = 3; i <= 10; i++) {
            String socialNumberPrefix = String.format("9501%02d", i);
            String phoneNumberSuffix = String.format("%04d", 5670 + i);
            
            User user = User.builder()
                    .username("testuser" + i)
                    .password("password123!")
                    .name("사용자" + i)
                    .socialNumber(socialNumberPrefix + "-1234567")
                    .phoneNumber("010-1234-" + phoneNumberSuffix)
                    .address("주소" + i)
                    .build();
            userRepository.save(user);
        }

        // when
        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);
        
        Page<User> firstPageResult = userRepository.findAll(firstPage);
        Page<User> secondPageResult = userRepository.findAll(secondPage);

        // then
        assertThat(firstPageResult.getContent()).hasSize(5);
        assertThat(secondPageResult.getContent()).hasSize(5);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(10);
        assertThat(firstPageResult.getTotalPages()).isEqualTo(2);
        assertThat(firstPageResult.isFirst()).isTrue();
        assertThat(secondPageResult.isLast()).isTrue();
    }

    @Test
    @DisplayName("UserService에서 사용하는 중복 검증 메서드 테스트")
    void duplicateValidationMethodsTest() {
        // given
        User user = User.builder()
            .username("duplicateTest")
            .password("password123!")
            .name("중복테스트")
            .socialNumber("951010-1234567")
            .phoneNumber("010-9999-9999")
            .address("중복테스트 주소")
            .build();
        
        entityManager.persistAndFlush(user);

        // when & then - existsByUsername 테스트
        assertThat(userRepository.existsByUsername("duplicateTest")).isTrue();
        assertThat(userRepository.existsByUsername("nonExistentUser")).isFalse();
        
        // when & then - existsBySocialNumber 테스트
        assertThat(userRepository.existsBySocialNumber("951010-1234567")).isTrue();
        assertThat(userRepository.existsBySocialNumber("000000-0000000")).isFalse();
    }

    @Test
    @DisplayName("대소문자 구분 사용자명 검색 테스트")
    void caseSensitiveUsernameTest() {
        // given
        User user = User.builder()
            .username("CaseSensitiveTest")
            .password("password123!")
            .name("케이스테스트")
            .socialNumber("941212-1234567")
            .phoneNumber("010-8888-8888")
            .address("케이스테스트 주소")
            .build();
        
        entityManager.persistAndFlush(user);

        // when & then
        assertThat(userRepository.existsByUsername("CaseSensitiveTest")).isTrue();
        assertThat(userRepository.existsByUsername("casesensitivetest")).isFalse();
        assertThat(userRepository.existsByUsername("CASESENSITIVETEST")).isFalse();
    }

    @Test
    @DisplayName("자동 생성 타임스탬프 검증")
    void autoGeneratedTimestampTest() {
        // given
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
        
        User user = User.builder()
            .username("timestampTest")
            .password("password123!")
            .name("타임스탬프테스트")
            .socialNumber("931111-1234567")
            .phoneNumber("010-7777-7777")
            .address("타임스탬프 테스트 주소")
            .build();

        // when
        User savedUser = userRepository.save(user);
        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isBetween(beforeSave, afterSave);
        assertThat(savedUser.getUpdatedAt()).isBetween(beforeSave, afterSave);
    }

    @Test
    @DisplayName("엔티티 업데이트 시 updatedAt 자동 갱신 테스트")
    void updateTimestampTest() {
        // given
        User user = User.builder()
            .username("updateTest")
            .password("password123!")
            .name("업데이트테스트")
            .socialNumber("921212-1234567")
            .phoneNumber("010-6666-6666")
            .address("업데이트 테스트 주소")
            .build();
        
        User savedUser = userRepository.save(user);
        entityManager.flush(); // DB에 반영
        entityManager.clear(); // 영속성 컨텍스트 초기화
        
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when
        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        foundUser.updateProfile("업데이트된이름", "010-0000-0000", "업데이트된주소");
        User updatedUser = userRepository.save(foundUser);
        entityManager.flush();

        // then
        assertThat(updatedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedUser.getName()).isEqualTo("업데이트된이름");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-0000-0000");
        assertThat(updatedUser.getAddress()).isEqualTo("업데이트된주소");
        assertThat(updatedUser.getCreatedAt()).isEqualTo(savedUser.getCreatedAt()); // createdAt은 변경되지 않음
    }

    @Test
    @DisplayName("한글 이름 검색 정확성 테스트")
    void koreanNameSearchTest() {
        // given - setUp에서 이미 testUser2(김철수)가 추가됨
        User koreanUser1 = User.builder()
            .username("korean1")
            .password("password123!")
            .name("김민수")
            .socialNumber("900101-1234567")
            .phoneNumber("010-1111-1111")
            .address("서울시")
            .build();
            
        User koreanUser2 = User.builder()
            .username("korean2")
            .password("password123!")
            .name("김민지")
            .socialNumber("900102-1234567")
            .phoneNumber("010-2222-2222")
            .address("부산시")
            .build();
            
        entityManager.persist(koreanUser1);
        entityManager.persist(koreanUser2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        Page<User> kimResults = userRepository.findByNameContaining("김", pageable);
        assertThat(kimResults.getContent()).hasSize(3); // testUser2(김철수) + korean1(김민수) + korean2(김민지)
        
        Page<User> minResults = userRepository.findByNameContaining("민", pageable);
        assertThat(minResults.getContent()).hasSize(2); // korean1(김민수) + korean2(김민지)
        
        Page<User> minsuResults = userRepository.findByNameContaining("민수", pageable);
        assertThat(minsuResults.getContent()).hasSize(1);
        assertThat(minsuResults.getContent().get(0).getName()).isEqualTo("김민수");
    }

    @Test
    @DisplayName("빈 결과 검색 테스트")
    void emptyResultSearchTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> emptyUsernameResult = userRepository.findByUsernameContaining("nonexistent", pageable);
        Page<User> emptyNameResult = userRepository.findByNameContaining("존재하지않는이름", pageable);
        Page<User> emptyBothResult = userRepository.findByUsernameContainingAndNameContaining("nonexistent", "존재하지않는이름", pageable);

        // then
        assertThat(emptyUsernameResult.getContent()).isEmpty();
        assertThat(emptyUsernameResult.getTotalElements()).isEqualTo(0);
        
        assertThat(emptyNameResult.getContent()).isEmpty();
        assertThat(emptyNameResult.getTotalElements()).isEqualTo(0);
        
        assertThat(emptyBothResult.getContent()).isEmpty();
        assertThat(emptyBothResult.getTotalElements()).isEqualTo(0);
    }
}