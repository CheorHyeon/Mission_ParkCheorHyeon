package com.ll.gramgram.boundedContext.likeablePerson.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@Controller
@RequestMapping("/usr/likeablePerson")
@RequiredArgsConstructor
public class LikeablePersonController {
    private final Rq rq;
    private final LikeablePersonService likeablePersonService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/like")
    public String showLike() {
        return "usr/likeablePerson/like";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/like")
    public String like(@Valid LikeForm likeForm) {
        RsData<LikeablePerson> rsData = likeablePersonService.like(rq.getMember(), likeForm.getUsername(), likeForm.getAttractiveTypeCode());

        if (rsData.isFail()) {
            return rq.historyBack(rsData);
        }

        return rq.redirectWithMsg("/usr/likeablePerson/list", rsData);
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

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String cancel(@PathVariable Long id) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElse(null);

        RsData canDeleteRsData = likeablePersonService.canCancel(rq.getMember(), likeablePerson);

        if (canDeleteRsData.isFail()) return rq.historyBack(canDeleteRsData);

        RsData deleteRsData = likeablePersonService.cancel(likeablePerson);

        if (deleteRsData.isFail()) return rq.historyBack(deleteRsData);

        return rq.redirectWithMsg("/usr/likeablePerson/list", deleteRsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String showModify(@PathVariable Long id, Model model) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElseThrow();

        RsData canModifyRsData = likeablePersonService.canModify(rq.getMember(), likeablePerson);

        if (canModifyRsData.isFail()) return rq.historyBack(canModifyRsData);

        model.addAttribute("likeablePerson", likeablePerson);

        return "usr/likeablePerson/modify";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String modify(@PathVariable Long id, @Valid ModifyForm modifyForm) {
        RsData<LikeablePerson> rsData = likeablePersonService.modifyAttractive(rq.getMember(), id, modifyForm.getAttractiveTypeCode());

        if (rsData.isFail()) {
            return rq.historyBack(rsData);
        }

        return rq.redirectWithMsg("/usr/likeablePerson/list", rsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/toList")
    public String showToList(Model model, @RequestParam(defaultValue = "") String gender, @RequestParam(defaultValue = "0") int attractiveTypeCode, @RequestParam(defaultValue = "1") int sortCode) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            // 혹시 모를 공백 제거(기본값이라면 오류남)
            if (!gender.equals(""))
                gender = gender.trim();
            // 호감 정보 가져오기
            Stream<LikeablePerson> likeablePeople = instaMember.getToLikeablePeople().stream();
            // 호감 표시한 사람의 성별로 필터링
            likeablePeople = filterByGender(likeablePeople, gender);
            // 호감 사유별 필터링
            likeablePeople = filterByAttractiveTypeCode(likeablePeople, attractiveTypeCode);
            // 정렬 코드별 정렬
            likeablePeople = sortBySortCode(likeablePeople, sortCode);

            model.addAttribute("likeablePeople", likeablePeople.collect(Collectors.toList()));
        }

        return "usr/likeablePerson/toList";
    }

    private Stream<LikeablePerson> sortBySortCode(Stream<LikeablePerson> likeablePeople, int sortCode) {
        switch (sortCode) {
            // 오래된 순이니 정렬(기존은 최신순 정렬 -> case 1은 고려x)
            // Id에 index가 있으니, 속도가 좀 더 빠름
            case 2 -> likeablePeople = likeablePeople
                    .sorted(Comparator.comparing(likeablePerson -> likeablePerson.getId()));

            // 인기 많은 순
            case 3 -> likeablePeople = likeablePeople
                    .sorted(comparing((LikeablePerson a) -> a.getToInstaMember().getLikes(), Comparator.reverseOrder())
                            .thenComparing(a -> a.getId(), Comparator.reverseOrder())
                    );

            // 인기 적은순
            case 4 -> likeablePeople = likeablePeople
                    .sorted(comparing((LikeablePerson a) -> a.getToInstaMember().getLikes())
                            .thenComparing(a -> a.getId(), Comparator.reverseOrder())
                    );

            // 성별순
            // 동일하면 최신순 -> CreateDate 대신 Id => index 활용
            case 5 -> likeablePeople = likeablePeople
                    // 알파벳 "M", "W" 순이니 역순으로 해야 여성부터
                    .sorted(Comparator.comparing((LikeablePerson lp) -> lp.getFromInstaMember().getGender(), Comparator.reverseOrder())
                            .thenComparing(lp -> lp.getId(), Comparator.reverseOrder())
                    );
            // 호감사유 순
            case 6 -> likeablePeople = likeablePeople
                    .sorted(Comparator.comparing(((LikeablePerson lp) -> lp.getAttractiveTypeCode()))
                            .thenComparing(lp -> lp.getId(), Comparator.reverseOrder())
                    );
        }

        return likeablePeople;
    }

    private Stream<LikeablePerson> filterByAttractiveTypeCode(Stream<LikeablePerson> likeablePeople, int attractiveTypeCode) {
        // 0인 경우는 "전체"를 갖도록 수정하였기에, 0인경우는 그대로 반환
        if (attractiveTypeCode == 0)
            return likeablePeople;

        // 값이 있는 경우는 호감 사유별 필터링
        return likeablePeople
                .filter(likeablePerson -> likeablePerson.getAttractiveTypeCode() == attractiveTypeCode);
    }

    private Stream<LikeablePerson> filterByGender(Stream<LikeablePerson> likeablePeople, String gender) {

        // 값이 없는경우는 전체를 뜻함으로 정렬 미시행
        if (gender.equals(""))
            return likeablePeople;

        // 값이 있는 경우는 성별 필터링, 호감 표시자(from)의 성별 검사하여 리스트화
        return likeablePeople
                .filter(likeablePerson -> likeablePerson.getFromInstaMember().getGender().equals(gender));
    }

    @AllArgsConstructor
    @Getter
    public static class LikeForm {
        @NotBlank
        @Size(min = 3, max = 30)
        private final String username;
        @NotNull
        @Min(1)
        @Max(3)
        private final int attractiveTypeCode;
    }

    @AllArgsConstructor
    @Getter
    public static class ModifyForm {
        @NotNull
        @Min(1)
        @Max(3)
        private final int attractiveTypeCode;
    }
}
