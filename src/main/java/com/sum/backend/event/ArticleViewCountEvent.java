package com.sum.backend.event;

/**
 * 게시글 조회로 조회수가 증가했음을 알리는 이벤트.
 * ES에는 viewCount 필드만 부분 업데이트(_update)하면 되므로 id와 조회수만 담는다.
 */
public record ArticleViewCountEvent(Long articleId, Long viewCount) {
}
