package com.ll.gramgram.boundedContext.notification.entity;

import com.ll.gramgram.base.baseEntity.BaseEntity;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {
    private LocalDateTime readDate;
    @ManyToOne
    @ToString.Exclude
    // 메세지 받는 사람(호감 받는 사람)
    private InstaMember toInstaMember;

    @ManyToOne
    @ToString.Exclude
    // 메세지를 발생시킨 행위를 한 사람(호감표시한 사람)
    private InstaMember fromInstaMember;

    private String typeCode;  // 호감표시=Like, 호감사유변경 = ModifyAttractiveType
    private String oldGender; // 해당사항 없으면 null
    private int oldAttractiveTypeCode; // 해당사항 없으면 null
    private String newGender; //  해당사항 없으면 null
    private int newAttractiveTypeCode; // 해당사항 없으면 null

    // 몇분전에 발생한지 표시하는 메서드 구현
    public String getCreateDateStrHuman() {
        Duration betweenTime = Duration.between(getCreateDate(), LocalDateTime.now());
        int hoursLeft = (int) betweenTime.toHours();
        int minsLeft = (int) betweenTime.toMinutes() % 60;

        if (hoursLeft == 0) {
            return "%d분전".formatted(minsLeft);
        }

        return "%d시간 %d분전".formatted(hoursLeft, minsLeft);
    }

    public Boolean isNonModify(){
        return typeCode.equals("Like");
    }

    public Boolean isModify(){
        return typeCode.equals("ModifyAttractiveType");
    }

    // 여자/남자 형식이므로 젠더값에 따라 표기 리턴값
    public String getGenderDisplayName() {
        return switch (fromInstaMember.getGender()) {
            case "W" -> "여자";
            default -> "남자";
        };
    }

    public String getNewAttractiveTypeCode() {
        return switch (newAttractiveTypeCode) {
            case 1 -> "외모";
            case 2 -> "성격";
            default -> "능력";
        };
    }
    public String getOldAttractiveTypeCode() {
        return switch (oldAttractiveTypeCode) {
            case 1 -> "외모";
            case 2 -> "성격";
            default -> "능력";
        };
    }

    // 알람 읽었을때 수정 메서드
    public void updateReadDate(LocalDateTime readDate) {
        this.readDate = readDate;
    }
}
