package com.sum.backend.event;

/**
 * 게시글이 삭제되어 ES 인덱스에서도 제거해야 함을 알리는 이벤트.
 */
public record ArticleDeleteEvent(Long articleId) {
}
