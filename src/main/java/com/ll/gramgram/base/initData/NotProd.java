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

                Member memberUser6ByKakao = memberService.whenSocialLogin("KAKAO", "KAKAO__2731659195").getData();
                Member memberUser7ByGoogle = memberService.whenSocialLogin("GOOGLE", "GOOGLE__116304245007543902962").getData();
                Member memberUser8ByNaver = memberService.whenSocialLogin("NAVER", "NAVER__nDt7SpLpzDpQzT6lbBWj3ZwY_zSDt-HAUSoFkjxBGoc").getData();
                Member memberUser9ByFacebook = memberService.whenSocialLogin("FACEBOOK", "FACEBOOK__5900099546783177").getData();

                instaMemberService.connect(memberUser2, "insta_user2", "M");
                instaMemberService.connect(memberUser3, "insta_user3", "W");
                instaMemberService.connect(memberUser4, "insta_user4", "M");
                instaMemberService.connect(memberUser5, "insta_user5", "W");

                instaMemberService.connect(memberUser6ByKakao, "insta_user6", "M");
                instaMemberService.connect(memberUser7ByGoogle, "insta_user7", "W");
                instaMemberService.connect(memberUser8ByNaver, "insta_user8", "M");
                instaMemberService.connect(memberUser9ByFacebook, "insta_user9", "W");

                LikeablePerson likeablePersonToInstaUser4 = likeablePersonService.like(memberUser3, "insta_user4", 1).getData();
                Ut.reflection.setFieldValue(likeablePersonToInstaUser4, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                LikeablePerson likeablePersonToInstaUser100 = likeablePersonService.like(memberUser3, "insta_user100", 2).getData();
                Ut.reflection.setFieldValue(likeablePersonToInstaUser100, "modifyUnlockDate", LocalDateTime.now().minusSeconds(1));

                LikeablePerson likeablePersonToInstaUserAbcd = likeablePersonService.like(memberUser3, "insta_user_abcd", 2).getData();

                LikeablePerson likeablePersonToInstaUser5 = likeablePersonService.like(memberUser2, "insta_user5", 2).getData();

                LikeablePerson likeablePersonToInstaUser4_2 = likeablePersonService.like(memberUser2, "insta_user4", 2).getData();
                LikeablePerson likeablePersonToInstaUser4_3 = likeablePersonService.like(memberUser5, "insta_user4", 3).getData();
                LikeablePerson likeablePersonToInstaUser4_4 = likeablePersonService.like(memberUser6ByKakao, "insta_user4", 2).getData();
                LikeablePerson likeablePersonToInstaUser4_5 = likeablePersonService.like(memberUser7ByGoogle, "insta_user4", 1).getData();
                LikeablePerson likeablePersonToInstaUser4_6 = likeablePersonService.like(memberUser8ByNaver, "insta_user4", 2).getData();
                LikeablePerson likeablePersonToInstaUser4_7 = likeablePersonService.like(memberUser9ByFacebook, "insta_user4", 3).getData();
            }
        };
    }
}
