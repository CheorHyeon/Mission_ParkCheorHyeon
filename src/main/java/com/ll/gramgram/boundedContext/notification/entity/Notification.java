package com.ll.gramgram.boundedContext.notification.entity;

import com.ll.gramgram.base.baseEntity.BaseEntity;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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


}
