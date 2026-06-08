package com.sum.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter // 데이터 수정이 필요한 필드를 위해 선언 (실무에서는 비즈니스 메서드로 대체 권장)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙을 위한 기본 생성자 (접근 제어로 안전성 확보)
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User author;

    @ManyToOne
    @JoinColumn(name = "board_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Board board;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
