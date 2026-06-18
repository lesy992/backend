package com.sum.backend.event;

import com.sum.backend.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 변경 이벤트를 받아 ES 인덱스에 반영하는 리스너.
 *
 * {@link TransactionalEventListener}(phase = AFTER_COMMIT) 로 동작하므로
 * DB 트랜잭션이 성공적으로 커밋된 뒤에만 실행된다.
 *  - DB는 롤백됐는데 ES에는 남는 불일치를 막는다.
 *  - ES 통신이 실패해도(예외는 ElasticsearchService 내부에서 처리) 이미 끝난
 *    DB 트랜잭션에는 영향을 주지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ArticleSearchSyncListener {

    private final ElasticsearchService elasticsearchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleIndex(ArticleIndexEvent event) {
        elasticsearchService.indexArticle(event.document());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleViewCount(ArticleViewCountEvent event) {
        elasticsearchService.updateViewCount(event.articleId(), event.viewCount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleDelete(ArticleDeleteEvent event) {
        elasticsearchService.deleteArticle(event.articleId());
    }
}
