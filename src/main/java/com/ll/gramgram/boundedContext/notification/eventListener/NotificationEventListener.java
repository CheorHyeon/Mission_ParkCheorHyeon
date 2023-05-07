package com.ll.gramgram.boundedContext.notification.eventListener;

import com.ll.gramgram.base.event.*;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationEventListener {
    private final NotificationService notificationService;

    // 호감사유 수정했을 때 알림 객체 생성하는 이벤트 리스너
    @EventListener
    public void listen(EventAfterModifyAttractiveType event) {

        LikeablePerson likeablePerson = event.getLikeablePerson();
        notificationService.makeModifyAttractive(likeablePerson, event.getOldAttractiveTypeCode());
        // 수정 전
        // notificationService.whenAfterModifyAttractiveType(event.getLikeablePerson(), event.getOldAttractiveTypeCode());
    }

    // 호감표시 했을 때 알림 객체 생성하는 이벤트 리스너
    @EventListener
    public void listen(EventAfterLike event) {
        LikeablePerson likeablePerson = event.getLikeablePerson();
        notificationService.makeLike(likeablePerson);
        // 수정 전
        // notificationService.whenAfterLike(event.getLikeablePerson());
    }

    // 사용자가 알림창 클릭했을때 발생하는 이벤트(readDate 업데이트) 리스너
    @EventListener
    public void listen(EventClickNotification event) {
        notificationService.whenClickNotification(event.getNotifications());
    }

}
