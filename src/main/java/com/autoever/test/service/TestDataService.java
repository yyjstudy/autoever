package com.autoever.test.service;

import com.autoever.member.entity.User;
import com.autoever.member.repository.UserRepository;
import com.autoever.test.dto.TestUserCreateRequest;
import com.autoever.test.dto.TestUserCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 테스트 데이터 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestDataService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 랜덤 이름 목록
    private static final List<String> FIRST_NAMES = Arrays.asList(
        "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "류", "전"
    );
    
    private static final List<String> LAST_NAMES = Arrays.asList(
        "민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "지후", "준서", "건우", "현우", "준혁", "지훈", "성민",
        "지우", "서연", "서윤", "지유", "서현", "예은", "하은", "민서", "하린", "지민", "시은", "소율", "지원", "예린", "다인",
        "태현", "민재", "성현", "동현", "우진", "지완", "승현", "준영", "정우", "영수", "철수", "영희", "순이", "맹구", "훈이"
    );
    
    private static final List<String> DOMAINS = Arrays.asList(
        "gmail.com", "naver.com", "daum.net", "kakao.com", "yahoo.com", "hotmail.com", "autoever.com"
    );
    
    private static final List<String> CITIES = Arrays.asList(
        "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시", 
        "경기도", "강원도", "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도"
    );
    
    private static final List<String> DISTRICTS = Arrays.asList(
        "중구", "종로구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구",
        "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구",
        "관악구", "서초구", "강남구", "송파구", "강동구", "수원시", "성남시", "안양시", "부천시", "광명시"
    );
    
    /**
     * 모든 유저 삭제
     */
    @Transactional
    public void deleteAllUsers() {
        long count = userRepository.count();
        userRepository.deleteAll();
        log.info("모든 유저 삭제 완료. 삭제된 유저 수: {}", count);
    }
    
    /**
     * 랜덤 유저 생성
     */
    @Transactional
    public TestUserCreateResponse createRandomUsers(TestUserCreateRequest request) {
        List<User> users = new ArrayList<>();
        List<TestUserCreateResponse.UserSummary> summaries = new ArrayList<>();
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedEmails = new HashSet<>();
        Set<String> usedPhoneNumbers = new HashSet<>();
        Set<String> usedSocialNumbers = new HashSet<>();
        
        for (int i = 0; i < request.userCount(); i++) {
            User user = createRandomUser(request.ageGroup(), usedUsernames, usedEmails, usedPhoneNumbers, usedSocialNumbers);
            users.add(user);
        }
        
        // 일괄 저장
        List<User> savedUsers = userRepository.saveAll(users);
        
        // 응답 생성
        for (User user : savedUsers) {
            int age = calculateAge(user.getSocialNumber());
            summaries.add(new TestUserCreateResponse.UserSummary(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                age
            ));
        }
        
        List<Long> userIds = savedUsers.stream().map(User::getId).toList();
        
        log.info("랜덤 유저 생성 완료. 생성된 유저 수: {}", savedUsers.size());
        
        return new TestUserCreateResponse(savedUsers.size(), userIds, summaries);
    }
    
    /**
     * 랜덤 유저 객체 생성
     */
    private User createRandomUser(Integer ageGroup, Set<String> usedUsernames, Set<String> usedEmails, 
                                 Set<String> usedPhoneNumbers, Set<String> usedSocialNumbers) {
        
        // 고유 사용자명 생성
        String username = generateUniqueUsername(usedUsernames);
        
        // 이름 생성
        String name = generateRandomName();
        
        // 나이와 주민등록번호 생성
        String socialNumber = generateSocialNumber(ageGroup, usedSocialNumbers);
        
        // 이메일 생성
        String email = generateUniqueEmail(username, usedEmails);
        
        // 전화번호 생성
        String phoneNumber = generateUniquePhoneNumber(usedPhoneNumbers);
        
        // 주소 생성
        String address = generateRandomAddress();
        
        // 기본 패스워드 (test123)
        String password = passwordEncoder.encode("test123");
        
        return User.builder()
                .username(username)
                .password(password)
                .name(name)
                .socialNumber(socialNumber)
                .email(email)
                .phoneNumber(phoneNumber)
                .address(address)
                .build();
    }
    
    /**
     * 고유 사용자명 생성
     */
    private String generateUniqueUsername(Set<String> usedUsernames) {
        String username;
        do {
            username = "testuser" + ThreadLocalRandom.current().nextInt(10000, 99999);
        } while (usedUsernames.contains(username) || userRepository.existsByUsername(username));
        
        usedUsernames.add(username);
        return username;
    }
    
    /**
     * 랜덤 이름 생성
     */
    private String generateRandomName() {
        String firstName = FIRST_NAMES.get(ThreadLocalRandom.current().nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(ThreadLocalRandom.current().nextInt(LAST_NAMES.size()));
        return firstName + lastName;
    }
    
    /**
     * 주민등록번호 생성 (연령대 기반)
     */
    private String generateSocialNumber(Integer ageGroup, Set<String> usedSocialNumbers) {
        String socialNumber;
        do {
            socialNumber = generateSocialNumberInternal(ageGroup);
        } while (usedSocialNumbers.contains(socialNumber) || userRepository.existsBySocialNumber(socialNumber));
        
        usedSocialNumbers.add(socialNumber);
        return socialNumber;
    }
    
    private String generateSocialNumberInternal(Integer ageGroup) {
        int currentYear = LocalDate.now().getYear();
        
        int birthYear;
        if (ageGroup != null) {
            // 연령대가 지정된 경우 (10, 20, 30, ..., 90)
            int minAge = ageGroup;
            int maxAge = ageGroup + 9;
            int age = ThreadLocalRandom.current().nextInt(minAge, maxAge + 1);
            birthYear = currentYear - age;
        } else {
            // 연령대가 지정되지 않은 경우 0~99세 랜덤
            int age = ThreadLocalRandom.current().nextInt(0, 100);
            birthYear = currentYear - age;
        }
        
        // 생년월일 생성
        String yearStr = String.format("%02d", birthYear % 100);
        String month = String.format("%02d", ThreadLocalRandom.current().nextInt(1, 13));
        String day = String.format("%02d", ThreadLocalRandom.current().nextInt(1, 29));
        
        // 성별코드 생성 (1900년대생: 1,2 / 2000년대생: 3,4)
        String genderCode;
        if (birthYear < 2000) {
            genderCode = ThreadLocalRandom.current().nextBoolean() ? "1" : "2";
        } else {
            genderCode = ThreadLocalRandom.current().nextBoolean() ? "3" : "4";
        }
        
        // 뒷자리 랜덤 생성
        String randomDigits = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        
        return yearStr + month + day + "-" + genderCode + randomDigits.substring(0, 6);
    }
    
    /**
     * 고유 이메일 생성
     */
    private String generateUniqueEmail(String username, Set<String> usedEmails) {
        String email;
        do {
            String domain = DOMAINS.get(ThreadLocalRandom.current().nextInt(DOMAINS.size()));
            email = username + "@" + domain;
        } while (usedEmails.contains(email) || userRepository.existsByEmail(email));
        
        usedEmails.add(email);
        return email;
    }
    
    /**
     * 고유 전화번호 생성
     */
    private String generateUniquePhoneNumber(Set<String> usedPhoneNumbers) {
        String phoneNumber;
        do {
            phoneNumber = "010-" + 
                         String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000)) + "-" +
                         String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
        } while (usedPhoneNumbers.contains(phoneNumber) || userRepository.existsByPhoneNumber(phoneNumber));
        
        usedPhoneNumbers.add(phoneNumber);
        return phoneNumber;
    }
    
    /**
     * 랜덤 주소 생성
     */
    private String generateRandomAddress() {
        String city = CITIES.get(ThreadLocalRandom.current().nextInt(CITIES.size()));
        String district = DISTRICTS.get(ThreadLocalRandom.current().nextInt(DISTRICTS.size()));
        int streetNumber = ThreadLocalRandom.current().nextInt(1, 999);
        int buildingNumber = ThreadLocalRandom.current().nextInt(1, 100);
        
        return city + " " + district + " " + streetNumber + "번길 " + buildingNumber;
    }
    
    /**
     * 나이 계산
     */
    private int calculateAge(String socialNumber) {
        if (socialNumber == null || socialNumber.length() < 8) {
            return 0;
        }
        
        int currentYear = LocalDate.now().getYear();
        int birthYear = Integer.parseInt(socialNumber.substring(0, 2));
        String genderCode = socialNumber.substring(7, 8);
        
        if (genderCode.equals("1") || genderCode.equals("2")) {
            birthYear += 1900;
        } else if (genderCode.equals("3") || genderCode.equals("4")) {
            birthYear += 2000;
        }
        
        return currentYear - birthYear;
    }
}