package com.ll.gramgram.boundedContext.notification.service;


import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public List<Notification> findByToInstaMember(InstaMember toInstaMember) {
        return notificationRepository.findByToInstaMember(toInstaMember);
    }

    // 호감 사유 수정 시 알림 객체 생성 메서드
    public void whenAfterModifyAttractiveType(LikeablePerson likeablePerson, int oldAttractiveTypeCode) {
        Notification modifyMsgNotification = Notification
                .builder()
                .toInstaMember(likeablePerson.getToInstaMember())
                .fromInstaMember(likeablePerson.getFromInstaMember())
                .typeCode("ModifyAttractiveType")
                .oldAttractiveTypeCode(oldAttractiveTypeCode)
                .newAttractiveTypeCode(likeablePerson.getAttractiveTypeCode())
                .build();

        notificationRepository.save(modifyMsgNotification);
    }

    // 호감 표시 알림 객체 생성 메서드
    public void whenAfterLike(LikeablePerson likeablePerson) {

        Notification likeMsgNotification = Notification
                .builder()
                .toInstaMember(likeablePerson.getToInstaMember())
                .fromInstaMember(likeablePerson.getFromInstaMember())
                .typeCode("Like")
                .newAttractiveTypeCode(likeablePerson.getAttractiveTypeCode())
                .build();

        notificationRepository.save(likeMsgNotification);
    }

    // 알림 클릭 시 readDate 업데이트 메서드
    public void whenClickNotification(List<Notification> notifications) {
        for (Notification notification : notifications) {
            // 조건 추가 (알람을 안읽었으면 수정하는 것, 과제 풀이 시 놓침)
            if (notification.getReadDate() == null) {
                notification.updateReadDate(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        }
    }

    // 테스트 사용 목적 메서드
    public Notification findById(Long id) {
        return notificationRepository.findById(id).get();
    }
}
