package com.sum.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 게시글 엔티티. DB의 article 테이블 한 행(row)에 매핑
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 내부적으로 쓰는 기본 생성자(외부 무분별한 생성 방지)
@AllArgsConstructor
@Builder
public class Article {

    @Id // 기본 키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 AUTO_INCREMENT 로 id 자동 생성
    private Long id;

    @Column(nullable = false) // NOT NULL 제약
    private String title;

    @Lob // 길이가 긴 텍스트(본문)를 위한 대용량 컬럼
    @Column(nullable = false)
    private String content;

    // 작성자: 여러 게시글이 한 명의 User 에 속함(N:1). FK 제약은 걸지 않음(NO_CONSTRAINT).
    @ManyToOne
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User author;

    // 소속 게시판: 여러 게시글이 한 게시판에 속함(N:1).
    // @JsonIgnore : 게시글을 JSON으로 응답할 때 board 정보는 빼고 내려준다(순환참조/불필요 노출 방지).
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "board_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Board board;

    @Column(nullable = false)
    private Long viewCount; // 조회수. 상세 조회할 때마다 +1

    @CreationTimestamp // 엔티티가 처음 저장될 때 현재 시각 자동 입력
    @Column(nullable = false, updatable = false) // 한번 생성되면 변경 불가
    private LocalDateTime createdAt;

    @UpdateTimestamp // 엔티티가 수정될 때마다 현재 시각으로 자동 갱신
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 제목/내용 수정용 메서드.
     * null 인 값은 건드리지 않아서, 보낸 항목만 부분 수정
     */
    public void update(String title, String content) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
    }
}
