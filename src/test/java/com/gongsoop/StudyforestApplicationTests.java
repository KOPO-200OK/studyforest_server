package com.gongsoop;

import com.gongsoop.member.controller.AuthController;
import com.gongsoop.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
class StudyforestApplicationTests {

    @MockitoBean
    private MemberService memberService;

    @Test
    void contextLoads() {
    }
}
