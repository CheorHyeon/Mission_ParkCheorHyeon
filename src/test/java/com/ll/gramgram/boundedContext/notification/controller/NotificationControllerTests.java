package com.ll.gramgram.boundedContext.notification.controller;


import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.boundedContext.member.controller.MemberController;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.repository.NotificationRepository;
import com.ll.gramgram.boundedContext.notification.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 스프링부트 관련 컴포넌트 테스트할 때 붙여야 함, Ioc 컨테이너 작동시킴
@AutoConfigureMockMvc // http 요청, 응답 테스트
@Transactional // 실제로 테스트에서 발생한 DB 작업이 영구적으로 적용되지 않도록, test + 트랜잭션 => 자동롤백
@ActiveProfiles("test") // application-test.yml 을 활성화 시킨다.
@TestMethodOrder(MethodOrderer.MethodName.class)
public class NotificationControllerTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private NotificationController notificationController;

    @Autowired
    NotificationService notificationService;

    @Autowired
    Rq rq;

    @Test
    @DisplayName("알람 읽음처리 확인")
    @WithUserDetails("user4")
    void t001() throws Exception {
        // WHEN
        ResultActions resultActions = mvc
                .perform(get("/usr/notification/list"));

        Notification notification = notificationService.findById(1L);
        assertThat(notification.getReadDate()).isNotNull();
    }

}
