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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

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

        if (!likeablePerson.isModifyUnlocked())
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
        if (likeablePerson == null) return RsData.of("F-1", "이미 취소되었습니다.");

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "취소할 권한이 없습니다.");

        if (!likeablePerson.isModifyUnlocked())
            return RsData.of("F-3", "아직 취소할 수 없습니다. %s 이후에 취소가 가능합니다.".formatted(likeablePerson.getModifyUnlockDateRemainStrHuman()));

        return RsData.of("S-1", "취소가 가능합니다.");
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
            if (!fromLikeablePerson.isModifyUnlocked())
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
        RsData canModifyRsData = canModify(actor, likeablePerson);

        if (canModifyRsData.isFail()) {
            return canModifyRsData;
        }

        String oldAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();
        String username = likeablePerson.getToInstaMember().getUsername();

        modifyAttractionTypeCode(likeablePerson, attractiveTypeCode);

        String newAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();

        return RsData.of("S-3", "%s님에 대한 호감사유를 %s에서 %s(으)로 변경합니다.".formatted(username, oldAttractiveTypeDisplayName, newAttractiveTypeDisplayName), likeablePerson);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, String username, int attractiveTypeCode) {
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

    public RsData canModify(Member actor, LikeablePerson likeablePerson) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (!Objects.equals(likeablePerson.getFromInstaMember().getId(), fromInstaMember.getId())) {
            return RsData.of("F-2", "해당 호감표시에 대해 사유변경을 수행할 권한이 없습니다.");
        }

        if (!likeablePerson.isModifyUnlocked()) {
            return RsData.of("F-3", "아직 호감사유변경을 할 수 없습니다. %s 후에는 가능합니다.".formatted(likeablePerson.getModifyUnlockDateRemainStrHuman()));
        }

        return RsData.of("S-1", "호감사유변경이 가능합니다.");
    }

    public List<LikeablePerson> findByToInstaMember(InstaMember instaMember, String gender, int attractiveTypeCode, int sortCode) {
        // 호감 정보 스트림 형태로 가져오기(메서드 연산 계속 Stream으로 하기에 중복로직 없애기)
        Stream<LikeablePerson> likeablePeople = instaMember.getToLikeablePeople().stream();
        // 호감 표시한 사람의 성별로 필터링
        likeablePeople = filterByGender(likeablePeople, gender);
        // 호감 사유별 필터링
        likeablePeople = filterByAttractiveTypeCode(likeablePeople, attractiveTypeCode);
        // 정렬 코드별 정렬
        likeablePeople = sortBySortCode(likeablePeople, sortCode);

        return likeablePeople.collect(Collectors.toList());

    }

    private Stream<LikeablePerson> sortBySortCode(Stream<LikeablePerson> likeablePeople, int sortCode) {
        likeablePeople = switch (sortCode) {
            // 오래된 순이니 정렬(기존은 최신순 정렬)
            // Id에 index가 있으니, 속도가 좀 더 빠름
            case 2 ->likeablePeople
                    .sorted(
                            Comparator.comparing(likeablePerson -> likeablePerson.getId())
                    );

            // 인기 많은 순
            case 3 -> likeablePeople
                    .sorted(
                            Comparator.comparing((LikeablePerson lp) -> lp.getFromInstaMember().getLikes()).reversed()
                                    .thenComparing(Comparator.comparing(LikeablePerson::getId).reversed())
                    );

            // 인기 적은순
            case 4 ->  likeablePeople
                    .sorted(
                            Comparator.comparing((LikeablePerson lp) -> lp.getFromInstaMember().getLikes())
                                    .thenComparing(Comparator.comparing(LikeablePerson::getId).reversed())
                    );

            // 성별순
            // 동일하면 최신순 -> CreateDate 대신 Id => index 활용
            case 5 -> likeablePeople
                    // 알파벳 "M", "W" 순이니 역순으로 해야 여성부터
                    .sorted(Comparator.comparing((LikeablePerson lp) -> lp.getFromInstaMember().getGender(), Comparator.reverseOrder())
                            .thenComparing(lp -> lp.getId(), Comparator.reverseOrder())
                    );
            // 호감사유 순
            case 6 -> likeablePeople
                    .sorted(Comparator.comparing(((LikeablePerson lp) -> lp.getAttractiveTypeCode()))
                            .thenComparing(lp -> lp.getId(), Comparator.reverseOrder())
                    );

            default -> likeablePeople;
        };

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

    // 테스트 케이스용 코드
    public List<LikeablePerson> findByToInstaMember(String username, String gender, int attractiveTypeCode, int sortCode) {
        return findByToInstaMember(instaMemberService.findByUsername(username).get(), gender, attractiveTypeCode, sortCode);
    }

    private Stream<LikeablePerson> filterByGender(Stream<LikeablePerson> likeablePeople, String gender) {

        // 값이 없는경우는 전체를 뜻함으로 정렬 미시행
        if (gender.equals(""))
            return likeablePeople;

        // 값이 있는 경우는 성별 필터링, 호감 표시자(from)의 성별 검사하여 리스트화
        return likeablePeople
                .filter(likeablePerson -> likeablePerson.getFromInstaMember().getGender().equals(gender));
    }
}