package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if (member.hasConnectedInstaMember() == false) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        // 기존 코드
        InstaMember fromInstaMember = member.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        // 호감표시 리스트 요소 검사(동일한 사용자에게 호감 표시를 했는지)
        for (LikeablePerson findMember : fromInstaMember.getFromLikeablePeople()) {
            // 중복인지 검사
            if (Objects.equals(findMember.getToInstaMember().getUsername(), username)) {
                // 매력까지 같으면 중복인 사람에게 호감 표시로 실패
                if (findMember.getAttractiveTypeCode() == attractiveTypeCode) {
                    return RsData.of("F-2", "중복 호감 표시가 불가능합니다.");
                }
                // 매력이 달라졌으면 호감 사유 변겅
                return modifyLike(findMember, fromInstaMember, toInstaMember, attractiveTypeCode);
            }
        }

        // 호감 표시자가 동일한 사람이 아닌 경우 실행 (기존 코드)
        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        // 너가 좋아하는 호감표시 생겼어.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        // 너를 좋아하는 호감표시 생겼어.
        toInstaMember.addToLikeablePerson(likeablePerson);

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    // Transactional으로 반환 후 객체 삭제하고 저장하도록 구현(save 하지 않고도)
    @Transactional
    public RsData<LikeablePerson> delete(LikeablePerson likeablePerson) {
        likeablePersonRepository.delete(likeablePerson);
        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }

    public RsData canActorDelete(Member actor, LikeablePerson likeablePerson) {
        if (likeablePerson == null) return RsData.of("F-1", "이미 삭제되었습니다.");

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "권한이 없습니다.");

        return RsData.of("S-1", "삭제가능합니다.");

    }

    // 동일한 사용자에 대한 호감 표시에 호감 표시 사유가 변경될 경우 메소드 구현
    @Transactional
    public RsData<LikeablePerson> modifyLike(LikeablePerson findmember, InstaMember fromInstaMember, InstaMember toInstaMember, int attractiveTypeCode) {

        LikeablePerson modifyLikeablePerson = findmember
                .toBuilder()
                .attractiveTypeCode(attractiveTypeCode) //  수정할 것만 넣어주면 됨
                .build();

        // save 전에 해줘야 함 : 영속성 컨텍스트에서 관리되는 findmember의 속성을 변경하고 저장(modifyLikeablePerson 자체를 저장 x)
        // 영속성 컨텍스트에서 그렇다 쳐도.. 객체 자체는 상관 없어서 save 이후에 해도 될 것 같은데 안됨...
        // 이유를 모르겠음..
        String beforeAttractive = findmember.getAttractiveTypeDisplayName();

        likeablePersonRepository.save(modifyLikeablePerson); // 저장

        // 같은 객체인지 확인용 코드 -> 출력 안됨 -> 수정 전, 후 객체가 다름을 확인
        if(Objects.equals(modifyLikeablePerson, findmember)) {
            System.out.println("제발 출력되지마라..!");
        }

        // 기존 리스트 삭제 : 위에 같은 객체 비교 했을때 다른 객체이기임을 확인하여 삭제 필요
        fromInstaMember.getFromLikeablePeople().remove(findmember);
        toInstaMember.getToLikeablePeople().remove(findmember);

        // 변경된 호감 표시 리스트 삽입
        fromInstaMember.addFromLikeablePerson(modifyLikeablePerson);
        toInstaMember.addToLikeablePerson(modifyLikeablePerson);

        // 호감 사유를 변경한 사용자명과 변경된 호감사유 저장 변수
        String changeInstaUsername = findmember.getToInstaMember().getUsername();
        String afterAttractive = modifyLikeablePerson.getAttractiveTypeDisplayName();
        return RsData.of("S-2", "%s에 대한 호감사유를 %s에서 %s으로 변경합니다.".formatted(changeInstaUsername, beforeAttractive, afterAttractive));
    }
}
