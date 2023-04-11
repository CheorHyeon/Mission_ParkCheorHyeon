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

    // 사용자 정보 삭제하면 호감 리스트 삭제되어야 하니 remove로 지정

    @OneToMany(mappedBy = "fromInstaMember", cascade = {CascadeType.REMOVE})
    @OrderBy("id desc") // 정렬, id는 fromLikeablePerson의 id를 기준 정렬
    @LazyCollection(LazyCollectionOption.EXTRA)  // 지연 로딩에 대해 즉시로딩하게 해줌, Count 실행(N+1 발생) 단 갯수만 파악하니 낫뱃
    @Builder.Default // @Buider가 있으면 new ArrayList<>(); 가 작동하지 않기에 이거 붙여야 함
    @ToString.Exclude
    private List<LikeablePerson> fromLikeablePeople = new ArrayList<>();

    // 사용자 정보 삭제하면 호감 리스트 삭제되어야 하니 remove로 지정
    @OneToMany(mappedBy = "toInstaMember", cascade = {CascadeType.REMOVE})
    @OrderBy("id desc") // 정렬, id는 toLikeablePerson의 id를 기준 정렬
    @LazyCollection(LazyCollectionOption.EXTRA)  // 지연 로딩에 대해 즉시로딩하게 해줌
    @Builder.Default // @Buider가 있으면 new ArrayList<>(); 가 작동하지 않기에 이거 붙여야 함
    private List<LikeablePerson> toLikeablePeople = new ArrayList<>();

    public void addFromLikeablePerson(LikeablePerson likeablePerson) {
        // 정렬 기준 : desc 이기에 제일 첫번째로 삽입
        fromLikeablePeople.add(0, likeablePerson);
    }

    public void addToLikeablePerson(LikeablePerson likeablePerson) {
        // 정렬 기준 : desc 이기에 제일 첫번째로 삽입
        toLikeablePeople.add(0, likeablePerson);
    }
}
