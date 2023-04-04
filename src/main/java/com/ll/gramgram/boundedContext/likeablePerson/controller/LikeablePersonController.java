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

    @PostMapping("/add")
    public String add(@Valid AddForm addForm) {
        RsData<LikeablePerson> createRsData = likeablePersonService.like(rq.getMember(), addForm.getUsername(), addForm.getAttractiveTypeCode());

        if (createRsData.isFail()) {
            return rq.historyBack(createRsData);
        }

        return rq.redirectWithMsg("/likeablePerson/list", createRsData);
    }

    @GetMapping("/list")
    public String showList(Model model) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            List<LikeablePerson> likeablePeople = likeablePersonService.findByFromInstaMemberId(instaMember.getId());
            model.addAttribute("likeablePeople", likeablePeople);
        }

        return "usr/likeablePerson/list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    @Transactional
    public String deleteList(Principal principal, @PathVariable("id") Long id) {
        // 생각한 전체 과정
        // (1) 삭제 하려는 호감 정보와 현재 로그인 한 사람의 정보를 가져온다.
        // (2) 삭제 하려는 likeable_person 테이블 내에 from insta id(올린 사람의 인스타 정보)와, 로그인 한 사람이 동일한 사람인지 비교
        // (3) (2)의 결과가 동일인이면 삭제, 아닐경우 오류 메시지 출력

        // (1) 삭제 하려는 호감 정보와 현재 로그인 한 사람의 정보를 가져온다.
        LikeablePerson likeablePerson = likeablePersonService.findById(id);
        Member member = memberService.findByUsername(principal.getName()).orElseThrow();

        // (1)-1 인스타 등록도 안되어있으면 호감 표시가 불가능하니 소유자가 될 수 없으므로 바로 실패 코드
        if (!member.hasConnectedInstaMember()) {
            return rq.redirectWithMsg("likeablePerson/list", RsData.of("F-1", "호감 표시자가 아닙니다."));
        }

        // (2) 삭제 하려는 likeable_person 테이블 내에 from insta id(올린 사람의 인스타 정보)와, 로그인 한 사람이 동일한 사람인지 비교
        if ((likeablePerson.getFromInstaMember().getId()) == (member.getInstaMember().getId())) {
            //(3) 동일한 사람임을 확인했으면 서비스에서 삭제 진행
            RsData<LikeablePerson> deletePerson = likeablePersonService.delete(likeablePerson);
            return rq.redirectWithMsg("likeablePerson/list", deletePerson);
        }

        return "usr/likeablePerson/list";
    }
}
