package com.ll.gramgram.boundedContext.likeablePerson.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/likeablePerson")
@RequiredArgsConstructor
public class LikeablePersonController {
    private final Rq rq;
    private final LikeablePersonService likeablePersonService;
    private final MemberService memberService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/add")
    public String showAdd() {
        return "usr/likeablePerson/add";
    }

    @AllArgsConstructor
    @Getter
    public static class AddForm {
        private final String username;
        private final int attractiveTypeCode;
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add")
    public String add(@Valid AddForm addForm) {
        RsData<LikeablePerson> createRsData = likeablePersonService.like(rq.getMember(), addForm.getUsername(), addForm.getAttractiveTypeCode());

        if (createRsData.isFail()) {
            return rq.historyBack(createRsData);
        }

        return rq.redirectWithMsg("/likeablePerson/list", createRsData);
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public String showList(Model model) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            // 해당 인스타회원이 좋아하는 사람들 목록
            List<LikeablePerson> likeablePeople = instaMember.getFromLikeablePeople();
            model.addAttribute("likeablePeople", likeablePeople);
        }

        return "usr/likeablePerson/list";
    }

    @PreAuthorize("isAuthenticated()") // 로그인을 하지 않은 경우는, 해당 어노테이션으로 고려대상이 아님(어노테이션 : 로그인 여부 검사)
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteList(@PathVariable("id") Long id) {
        // 생각한 전체 과정
        // (1) 삭제 하려는 호감 정보와 현재 로그인 한 사람의 정보를 가져온다.
        // (2) 삭제 하려는 likeable_person 테이블 내에 from insta id(올린 사람의 인스타 정보)와, 로그인 한 사람이 동일한 사람인지 비교
        // (3) (2)의 결과가 동일인이면 삭제, 아닐경우 오류 메시지 출력
        // 로그인을 하지 않은 경우

        // (1) 삭제 하려는 호감 정보와 현재 로그인 한 사람의 정보를 가져온다.
        // (1)-1 삭제 하려는 호감 정보를 가져온다.
        LikeablePerson likeablePerson = likeablePersonService.findById(id);

        // (1)-1 @호감 표시 데이터가 없는 경우 오류 처리
        if(likeablePerson == null) {
            return rq.historyBack("등록한 호감정보가 없습니다.");
        }

        // (1)-2 현재 로그인 한 사람의 정보를 가져온다.
        Member member = memberService.findByUsername((rq.getMember()).getUsername()).orElseThrow();

        // (1)-2 @ 로그인 한 회원 인스타 등록도 안되어있으면 호감 표시가 불가능하니 소유자가 될 수 없으므로 바로 실패 코드
        if (!member.hasConnectedInstaMember()) {
            return rq.redirectWithMsg("likeablePerson/list", RsData.of("F-1", "인스타ID를 먼저 등록해주세요"));
        }

        // (2) 호감표시한 사람과 로그인한 멤버가 같은지 비교(올린 사람과 현재 사용자가 같지 않은 경우 오류)
        if ((likeablePerson.getFromInstaMember().getId()) != (member.getInstaMember().getId())) {
            return rq.historyBack("사용자가 표시한 호감이 아닙니다.");
        }

        // (3) 1~2조건들이 모두 거짓이라면, 삭제할 조건 충족
            RsData<LikeablePerson> deletePerson = likeablePersonService.delete(likeablePerson);

            if(deletePerson.isFail())
                return rq.historyBack(deletePerson);

            return rq.redirectWithMsg("/likeablePerson/list", deletePerson);
    }
}
