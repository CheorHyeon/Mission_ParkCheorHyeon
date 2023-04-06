package com.ll.gramgram.boundedContext.instaMember.entity;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.core.annotation.Order;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString
@Entity
@Getter
public class InstaMember {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;
    @Column(unique = true)
    private String username;
    @Setter
    private String gender;

    @OneToMany(mappedBy = "fromInstaMember", cascade = {CascadeType.ALL})
    @OrderBy("id desc") // 정렬, id는 fromLikeablePerson의 id를 기준 정렬
    @LazyCollection(LazyCollectionOption.EXTRA)  // 지연 로딩에 대해 즉시로딩하게 해줌
    @Builder.Default // @Buider가 있으면 new ArrayList<>(); 가 작동하지 않기에 이거 붙여야 함
    private List<LikeablePerson> fromLikeablePeople = new ArrayList<>();
}
