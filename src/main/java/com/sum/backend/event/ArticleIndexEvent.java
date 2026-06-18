package com.sum.backend.event;

import com.sum.backend.dto.ArticleDocument;

/**
 * 게시글이 생성/수정되어 ES에 색인(갱신)해야 함을 알리는 이벤트.
 *
 * 트랜잭션 커밋 이후에 ES로 보내야 하는데, 그 시점엔 영속성 컨텍스트가 닫혀
 * 엔티티의 지연 로딩 연관관계(author/board)에 접근하면 예외가 난다.
 * 그래서 트랜잭션 안에서 미리 변환해 둔 {@link ArticleDocument}를 담아 전달한다.
 */
public record ArticleIndexEvent(ArticleDocument document) {
}
