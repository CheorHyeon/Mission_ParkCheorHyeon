package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.base.event.EventAfterLike;
import com.ll.gramgram.base.event.EventAfterModifyAttractiveType;
import com.ll.gramgram.base.event.EventBeforeCancelLike;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public RsData<LikeablePerson> like(Member actor, String username, int attractiveTypeCode) {
        RsData canLikeRsData = canLike(actor, username, attractiveTypeCode);

        if (canLikeRsData.isFail()) return canLikeRsData;

        if (canLikeRsData.getResultCode().equals("S-2")) return modifyAttractive(actor, username, attractiveTypeCode);

        InstaMember fromInstaMember = actor.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(actor.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .modifyUnlockDate(AppConfig.genLikeablePersonModifyCoolTime())
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        // 너가 좋아하는 호감표시 생겼어.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        // 너를 좋아하는 호감표시 생겼어.
        toInstaMember.addToLikeablePerson(likeablePerson);

        publisher.publishEvent(new EventAfterLike(this, likeablePerson));

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }

    @Transactional
    public RsData cancel(LikeablePerson likeablePerson) {

        if(!likeablePerson.isModifyUnlocked())
            return RsData.of("F-9", "호감 업데이트 이후 3시간이 지나야 삭제가 가능합니다.");

        publisher.publishEvent(new EventBeforeCancelLike(this, likeablePerson));

        // 너가 생성한 좋아요가 사라졌어.
        likeablePerson.getFromInstaMember().removeFromLikeablePerson(likeablePerson);

        // 너가 받은 좋아요가 사라졌어.
        likeablePerson.getToInstaMember().removeToLikeablePerson(likeablePerson);

        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    public RsData canCancel(Member actor, LikeablePerson likeablePerson) {
        if (likeablePerson == null) return RsData.of("F-1", "이미 삭제되었습니다.");

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "권한이 없습니다.");

        if(!likeablePerson.isModifyUnlocked())
            return RsData.of("F-9", "호감 표시 또는 사유 변경 후 3시간 이후 취소가 가능합니다.");

        return RsData.of("S-1", "삭제가능합니다.");
    }

    private RsData canLike(Member actor, String username, int attractiveTypeCode) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (fromInstaMember.getUsername().equals(username)) {
            return RsData.of("F-2", "본인을 호감상대로 등록할 수 없습니다.");
        }

        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = fromInstaMember.getFromLikeablePeople();

        // 그 중에서 좋아하는 상대가 username 인 녀석이 혹시 있는지 체크
        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson != null && fromLikeablePerson.getAttractiveTypeCode() == attractiveTypeCode) {
            return RsData.of("F-3", "이미 %s님에 대해서 호감표시를 했습니다.".formatted(username));
        }

        long likeablePersonFromMax = AppConfig.getLikeablePersonFromMax();

        if (fromLikeablePerson != null) {
            // 호감 등록페이지에서 동일한 사람에 대해 호감 표시 할 경우에도 쿨타임 검사(없어도 되지만, 굳이 다른 함수 호출 안하도록)
            if(!fromLikeablePerson.isModifyUnlocked())
                return RsData.of("F-9", "%s님에 대한 호감 사유 수정은 등록 후 3시간 이후 가능합니다.".formatted(username));

            return RsData.of("S-2", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
        }

        if (fromLikeablePeople.size() >= likeablePersonFromMax) {
            return RsData.of("F-4", "최대 %d명에 대해서만 호감표시가 가능합니다.".formatted(likeablePersonFromMax));
        }

        return RsData.of("S-1", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
    }

    public Optional<LikeablePerson> findByFromInstaMember_usernameAndToInstaMember_username(String fromInstaMemberUsername, String toInstaMemberUsername) {
        return likeablePersonRepository.findByFromInstaMember_usernameAndToInstaMember_username(fromInstaMemberUsername, toInstaMemberUsername);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, Long id, int attractiveTypeCode) {
        Optional<LikeablePerson> likeablePersonOptional = findById(id);

        if (likeablePersonOptional.isEmpty()) {
            return RsData.of("F-1", "존재하지 않는 호감표시입니다.");
        }

        LikeablePerson likeablePerson = likeablePersonOptional.get();

        return modifyAttractive(actor, likeablePerson, attractiveTypeCode);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, LikeablePerson likeablePerson, int attractiveTypeCode) {
        RsData canModifyRsData = canModifyLike(actor, likeablePerson);

        if (canModifyRsData.isFail()) {
            return canModifyRsData;
        }

        String oldAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();
        String username = likeablePerson.getToInstaMember().getUsername();

        modifyAttractionTypeCode(likeablePerson, attractiveTypeCode);

        String newAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();

        return RsData.of("S-3", "%s님에 대한 호감사유를 %s에서 %s(으)로 변경합니다.".formatted(username, oldAttractiveTypeDisplayName, newAttractiveTypeDisplayName), likeablePerson);
    }

    private RsData<LikeablePerson> modifyAttractive(Member actor, String username, int attractiveTypeCode) {
        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = actor.getInstaMember().getFromLikeablePeople();

        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson == null) {
            return RsData.of("F-7", "호감표시를 하지 않았습니다.");
        }

        return modifyAttractive(actor, fromLikeablePerson, attractiveTypeCode);
    }

    private void modifyAttractionTypeCode(LikeablePerson likeablePerson, int attractiveTypeCode) {
        int oldAttractiveTypeCode = likeablePerson.getAttractiveTypeCode();
        RsData rsData = likeablePerson.updateAttractionTypeCode(attractiveTypeCode);

        if (rsData.isSuccess()) {
            publisher.publishEvent(new EventAfterModifyAttractiveType(this, likeablePerson, oldAttractiveTypeCode, attractiveTypeCode));
        }
    }

    public RsData canModifyLike(Member actor, LikeablePerson likeablePerson) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (!Objects.equals(likeablePerson.getFromInstaMember().getId(), fromInstaMember.getId())) {
            return RsData.of("F-2", "해당 호감표시를 수정할 권한이 없습니다.");
        }

        if(!likeablePerson.isModifyUnlocked()){
            return RsData.of("F-8", "호감 표시 후 3시간 이후 호감 사유 수정이 가능합니다.");
        }

        return RsData.of("S-1", "호감 사유 수정이 가능합니다.");
    }
}