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
import java.util.Objects;

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

        // 컨트롤러 단에서 간단한 작업(호감 표시 인원 파악) 입구 컷(등록폼도 안보이게)
        if(IsSizeFull(rq.getMember()) == true) {
            return rq.redirectWithMsg("/likeablePerson/list", "최대 10명 까지 호감 표시가 가능합니다. 사유 변경을 희망하시면 삭제 후 재등록 바랍니다.");
        }

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
        // 호감 표시 데이터 생성
        // 2주차 1번 미션, 중복 호감 표시 여부 검사(case 4) + 중복 아이디 일 경우 사유 검사 후 호감 사유 변경

        // 10명을 초과하지 않았다면 서비스 단에서 로직 수행(사용자 입력값에 대한 관여x)
        RsData<LikeablePerson> createRsData = likeablePersonService.like(rq.getMember(), addForm.getUsername(), addForm.getAttractiveTypeCode());

        // 실패코드인 경우(중복-호감 사유 동일, 본인, 인스타 아이디 미등록 등) 히스토리백
        if (createRsData.isFail()) {
            return rq.historyBack(createRsData);
        }

        return rq.redirectWithMsg("/likeablePerson/list", createRsData);
    }

    // 호감표시 인원 검사를 위한 메소드
    private boolean IsSizeFull(Member logindMember) {
        if(logindMember.getInstaMember().getFromLikeablePeople().size() >= 10) {
            return true;
        }

        return false;
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
    public String delete(@PathVariable("id") Long id) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElse(null);

        RsData canActorDeleteRsData = likeablePersonService.canActorDelete(rq.getMember(), likeablePerson);

        if (canActorDeleteRsData.isFail()) return rq.historyBack(canActorDeleteRsData);

        RsData deleteRsData = likeablePersonService.delete(likeablePerson);

        if (deleteRsData.isFail()) return rq.historyBack(deleteRsData);

        return rq.redirectWithMsg("/likeablePerson/list", deleteRsData);
    }
}
