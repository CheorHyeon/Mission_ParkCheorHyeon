package com.ll.gramgram.base.event;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

// 클릭 이벤트 객체
@Getter
public class EventClickNotification extends ApplicationEvent {
    private final List<Notification> notifications;

    public EventClickNotification(Object source, List<Notification> notifications) {
        super(source);
        this.notifications = notifications;
    }
}
