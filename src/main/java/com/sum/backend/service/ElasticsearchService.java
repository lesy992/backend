package com.sum.backend.service;

import com.sum.backend.dto.ArticleDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 연동을 담당하는 서비스.
 *
 * 검색 인덱스는 "있으면 좋은" 부가 기능이므로, ES 통신이 실패하더라도
 * 게시글 작성/수정/삭제 같은 핵심 DB 작업까지 같이 실패하면 안 된다.
 * 따라서 인덱싱/삭제 메서드는 예외를 밖으로 던지지 않고 로그만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private static final String ARTICLE_INDEX = "articles";

    /** ES 한 번에 가져올 검색 결과(id) 최대 개수. 페이징은 MySQL 단계에서 처리한다. */
    private static final int MAX_SEARCH_IDS = 1000;

    private final RestClient elasticsearchRestClient;

    /**
     * Article 문서를 'articles' 인덱스에 추가 또는 갱신.
     * PUT /articles/_doc/{id}
     *
     * 지연 로딩 문제를 피하기 위해 엔티티가 아니라 트랜잭션 안에서 미리 만든
     * {@link ArticleDocument}를 받는다.
     */
    public void indexArticle(ArticleDocument document) {
        try {
            elasticsearchRestClient.put()
                    .uri("/{index}/_doc/{id}", ARTICLE_INDEX, document.getId())
                    .body(document) // 내부 Jackson에 의해 자동으로 JSON 변환됨
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("게시글 ES 인덱싱 실패. articleId={}", document.getId(), e);
        }
    }

    /**
     * 조회수만 부분 업데이트.
     * POST /articles/_update/{id}  body: {"doc": {"viewCount": n}}
     *
     * 전체 문서를 다시 색인하지 않고 viewCount 필드만 갱신한다.
     * 아직 색인되지 않은 글이면 404가 날 수 있는데, 검색 부가 기능이므로 로그만 남긴다.
     */
    public void updateViewCount(Long articleId, Long viewCount) {
        try {
            elasticsearchRestClient.post()
                    .uri("/{index}/_update/{id}", ARTICLE_INDEX, articleId)
                    .body(Map.of("doc", Map.of("viewCount", viewCount)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("게시글 ES 조회수 갱신 실패. articleId={}", articleId, e);
        }
    }

    /**
     * Article 문서를 'articles' 인덱스에서 삭제.
     * DELETE /articles/_doc/{id}
     */
    public void deleteArticle(Long articleId) {
        try {
            elasticsearchRestClient.delete()
                    .uri("/{index}/_doc/{id}", ARTICLE_INDEX, articleId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("게시글 ES 삭제 실패. articleId={}", articleId, e);
        }
    }

    /**
     * 키워드로 (게시판 구분 없이) 전체 게시글을 검색해서 매칭된 게시글 id 목록만 반환.
     * POST /articles/_search
     *
     * - multi_match : title, content 두 필드를 동시에 검색
     *
     * 페이징은 ES 가 아니라 MySQL 조회 단계에서 처리하므로, 여기서는 매칭되는 id 를 한 번에 가져온다.
     * 다만 ES 는 size 를 지정하지 않으면 기본 10건만 반환하므로, 충분히 큰 size 로 상한을 둔다.
     * (검색 결과가 {@link #MAX_SEARCH_IDS} 건을 넘으면 초과분은 잘린다 — 학습용 규모에서는 충분)
     *
     * 실제 게시글 내용은 MySQL 에서 다시 조회하므로, ES 에서는 본문(_source)을 받지 않고
     * "_source": false 로 두어 문서 id(_id)만 가져온다. (네트워크 전송량 최소화)
     * <pre>
     * {
     *   "_source": false,
     *   "size": <MAX_SEARCH_IDS>,
     *   "query": {
     *     "multi_match": { "query": "<keyword>", "fields": ["title", "content"] }
     *   }
     * }
     * </pre>
     */
    public List<Long> search(String keyword) {
        Map<String, Object> searchQuery = Map.of(
                "_source", false,
                "size", MAX_SEARCH_IDS,
                "query", Map.of(
                        "multi_match", Map.of(
                                "query", keyword,
                                "fields", List.of("title", "content")
                        )
                )
        );

        Map<String, Object> response = elasticsearchRestClient.post()
                .uri("/{index}/_search", ARTICLE_INDEX)
                .body(searchQuery)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return parseIds(response);
    }

    /** ES 검색 응답(hits.hits[]._id)에서 게시글 id 목록을 추출. */
    @SuppressWarnings("unchecked")
    private List<Long> parseIds(Map<String, Object> response) {
        List<Long> ids = new ArrayList<>();
        if (response == null) {
            return ids;
        }

        Map<String, Object> hits = (Map<String, Object>) response.get("hits");
        if (hits == null) {
            return ids;
        }

        List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
        if (hitList == null) {
            return ids;
        }

        for (Map<String, Object> hit : hitList) {
            Object id = hit.get("_id"); // 색인 시 article.getId() 를 문서 id 로 사용했으므로 문자열 형태
            if (id != null) {
                ids.add(Long.valueOf(id.toString()));
            }
        }
        return ids;
    }
}
