package com.autoever.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈 페이지 컨트롤러
 * 루트 경로 접근 시 Swagger UI로 리다이렉트
 */
@Controller
public class HomeController {

    /**
     * 루트 경로 접근 시 Swagger UI로 리다이렉트
     * 
     * @return Swagger UI 리다이렉트 경로
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }
}