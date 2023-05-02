package com.ll.gramgram.boundedContext.notification.service;


import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMemberSnapshot;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public List<Notification> findByToInstaMember(InstaMember toInstaMember) {
        return notificationRepository.findByToInstaMember(toInstaMember);
    }

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
}
