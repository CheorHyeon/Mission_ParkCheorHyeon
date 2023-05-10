package com.ll.gramgram.base.initData;

import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import com.ll.gramgram.standard.util.Ut;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
@Profile({"dev", "test"})
public class NotProd {
    @Bean
    CommandLineRunner initData(
            MemberService memberService,
            InstaMemberService instaMemberService,
            LikeablePersonService likeablePersonService
    ) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                Member memberAdmin = memberService.join("admin", "1234").getData();
                Member memberUser1 = memberService.join("user1", "1234").getData();
                Member memberUser2 = memberService.join("user2", "1234").getData();
                Member memberUser3 = memberService.join("user3", "1234").getData();
                Member memberUser4 = memberService.join("user4", "1234").getData();
                Member memberUser5 = memberService.join("user5", "1234").getData();
                Member memberUser8 = memberService.join("user8", "1234").getData();

                Member memberUser6ByKakao = memberService.whenSocialLogin("KAKAO", "KAKAO__2731659195").getData();
                Member memberUser7ByGoogle = memberService.whenSocialLogin("GOOGLE", "GOOGLE__116304245007543902962").getData();
                Member memberUser8ByNaver = memberService.whenSocialLogin("NAVER", "NAVER__nDt7SpLpzDpQzT6lbBWj3ZwY_zSDt-HAUSoFkjxBGoc").getData();
                Member memberUser9ByFacebook = memberService.whenSocialLogin("FACEBOOK", "FACEBOOK__5900099546783177").getData();

                instaMemberService.connect(memberUser2, "insta_user2", "M");
                instaMemberService.connect(memberUser3, "insta_user3", "W");
                instaMemberService.connect(memberUser4, "insta_user4", "M");
                instaMemberService.connect(memberUser5, "insta_user5", "W");
                instaMemberService.connect(memberUser1, "insta_user1", "M");

                // 원활한 테스트와 개발을 위해서 자동으로 만들어지는 호감이 삭제, 수정이 가능하도록 쿨타임해제
                LikeablePerson likeablePersonToinstaUser4 = likeablePersonService.like(memberUser3, "insta_user4", 3).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser4, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));
                LikeablePerson likeablePersonToinstaUser100 = likeablePersonService.like(memberUser3, "insta_user100", 2).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser100, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                LikeablePerson likeablePersonToinstaUser4_2 = likeablePersonService.like(memberUser2, "insta_user4", 1).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser4_2, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));
                LikeablePerson likeablePersonToinstaUser4_3 = likeablePersonService.like(memberUser5, "insta_user4", 2).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser4_3, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                // user2에 3명
                LikeablePerson likeablePersonToinstaUser2_1 = likeablePersonService.like(memberUser3, "insta_user2", 1).getData();

                LikeablePerson likeablePersonToinstaUser2_2 = likeablePersonService.like(memberUser1, "insta_user2", 2).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser2_2, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));
                LikeablePerson likeablePersonToinstaUser2_3 = likeablePersonService.like(memberUser4, "insta_user2", 3).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser2_3, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                //user3에 2명
                LikeablePerson likeablePersonToinstaUser3_1 = likeablePersonService.like(memberUser2, "insta_user3", 1).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser3_1, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));
                LikeablePerson likeablePersonToinstaUser3_2 = likeablePersonService.like(memberUser1, "insta_user3", 2).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser3_2, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                //user5에 1명
                LikeablePerson likeablePersonToinstaUser5_1 = likeablePersonService.like(memberUser3, "insta_user5", 1).getData();
                Ut.reflection.setFieldValue(likeablePersonToinstaUser5_1, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                // 쿨타임 테스트용
                LikeablePerson likeablePersonToInstaUserAbcd = likeablePersonService.like(memberUser3, "insta_user_abcd", 2).getData();
            }
        };
    }
}
