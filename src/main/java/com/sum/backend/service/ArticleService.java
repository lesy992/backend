package com.sum.backend.service;

import com.sum.backend.dto.ArticleDocument;
import com.sum.backend.dto.CreateArticle;
import com.sum.backend.dto.EditArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.entity.Board;
import com.sum.backend.entity.User;
import com.sum.backend.event.ArticleDeleteEvent;
import com.sum.backend.event.ArticleIndexEvent;
import com.sum.backend.event.ArticleViewCountEvent;
import com.sum.backend.repository.ArticleRepository;
import com.sum.backend.repository.BoardRepository;
import com.sum.backend.repository.CommentRepository;
import com.sum.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글(Article) 관련 실제 비즈니스 로직을 담당하는 서비스.
 *
 * @Transactional : 메서드 실행 동안 하나의 DB 트랜잭션으로 묶는다.
 *  - 중간에 예외가 나면 모두 롤백된다.
 *  - 엔티티 값을 바꾸면(setter 등) 트랜잭션이 끝날 때 JPA가 변경을 감지(더티 체킹)해 자동으로 UPDATE 한다.
 *  - readOnly = true 면 "조회 전용"이라 변경 감지를 하지 않아(=DB에 쓰지 않아) 성능상 유리하다.
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ElasticsearchService elasticsearchService;

    /**
     * 게시글 작성.
     * 1) 로그인 ID로 작성자(User) 조회 → 없으면 예외
     * 2) 글을 올릴 게시판(Board) 조회 → 없으면 예외
     * 3) 빌더로 새 게시글을 만들어(조회수 0으로 시작) 저장
     */
    @Transactional
    public Article writeArticle(CreateArticle article, String loginId) {
        User author = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(article.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        Article newArticle = Article.builder()
                .author(author)
                .board(board)
                .title(article.getTitle())
                .content(article.getContent())
                .viewCount(0L)
                .build();

        Article savedArticle = articleRepository.save(newArticle);
        // 트랜잭션 안에서 문서로 변환해 두고, 커밋 이후 ES에 색인하도록 이벤트 발행
        eventPublisher.publishEvent(new ArticleIndexEvent(ArticleDocument.fromEntity(savedArticle)));
        return savedArticle;
    }

    /**
     * 게시글 1개 상세 조회 + 조회수 1 증가.
     * 1) 게시글 조회 → 없으면 예외
     * 2) 요청한 게시판(boardId)에 실제로 속한 글인지 검증 (URL 위조 방지)
     * 3) DB에서 조회수를 원자적으로 +1 (동시에 여러 명이 봐도 누락되지 않음)
     * 4) 증가된 값이 반영된 게시글을 다시 조회해 반환
     *    (increaseViewCount 가 영속성 컨텍스트를 비우므로, 다시 조회하면 최신 조회수가 들어온다)
     */
    @Transactional
    public Article getArticle(Long boardId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        // DB에서 원자적으로 조회수 증가 후, 증가된 값이 반영된 게시글을 다시 조회해 반환
        articleRepository.increaseViewCount(articleId);
        Article updated = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 커밋 이후 ES의 viewCount만 부분 업데이트하도록 이벤트 발행
        eventPublisher.publishEvent(new ArticleViewCountEvent(updated.getId(), updated.getViewCount()));

        return updated;
    }

    /** 특정 게시판의 게시글 목록 (최신순 첫 페이지). 조회만 하므로 readOnly. */
    @Transactional(readOnly = true)
    public List<Article> getArticles(Long boardId, Pageable pageable) {
        return articleRepository.findByBoardId(boardId, pageable);
    }

    /** 기준 글(articleId)보다 "이전(오래된)" 글들을 조회. 무한 스크롤에서 아래로 더 보기. */
    @Transactional
    public List<Article> getArticlesForOld(Long boardId, Long articleId, Pageable pageable) {
        return articleRepository.findArticlesForOld(boardId, articleId, pageable);
    }

    /** 기준 글(articleId)보다 "이후(최신)" 글들을 조회. 무한 스크롤에서 위로 새로고침. */
    @Transactional
    public List<Article> getArticlesForNew(Long boardId, Long articleId, Pageable pageable) {
        return articleRepository.findArticlesForNew(boardId, articleId, pageable);
    }

    /**
     * 게시글 수정. 본인이 쓴 글만 수정 가능.
     * 검증 순서: 요청자 확인 → 게시판 확인 → 게시글 확인 → 게시판 일치 확인 → 작성자 본인 확인.
     * article.update(...) 로 값만 바꿔두면 트랜잭션 종료 시 JPA가 자동으로 UPDATE 한다(별도 save 불필요).
     */
    @Transactional
    public Article editArticle(Long boardId, Long articleId, EditArticle editArticle, String loginId) {

        // 1. 수정 요청자 조회
        User requestUser = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 게시판 존재 여부 확인 (필요시)
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        // 3. 수정할 게시글 조회
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 4. 요청된 게시판(boardId)과 실제 게시글의 게시판 일치 여부 검증
        if (!article.getBoard().getId().equals(board.getId())) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        // 5. 작성자 권한 검증 (요청자와 게시글 작성자가 같은지 확인)
        if (!article.getAuthor().getId().equals(requestUser.getId())) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }

        // 6. 값 변경 → 트랜잭션 종료 시 더티 체킹으로 자동 UPDATE
        article.update(editArticle.getTitle(), editArticle.getContent());

        // 변경 내용을 커밋 이후 ES에 재색인하도록 이벤트 발행
        eventPublisher.publishEvent(new ArticleIndexEvent(ArticleDocument.fromEntity(article)));

        return article;
    }

    /**
     * 게시글 삭제. 본인이 쓴 글만 삭제 가능.
     * 게시글을 지우기 전에, 그 글에 달린 댓글들을 먼저 삭제한다(자식 데이터 정리).
     */
    @Transactional
    public void deleteArticle(Long boardId, Long articleId, String loginId) {
        User requestUser = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(board.getId())) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        if (!article.getAuthor().getId().equals(requestUser.getId())) {
            throw new IllegalArgumentException("게시글 삭제 권한이 없습니다.");
        }

        commentRepository.deleteByArticleId(articleId); // 댓글 먼저 삭제
        articleRepository.delete(article);              // 그 다음 게시글 삭제

        // 커밋 이후 ES 인덱스에서도 제거하도록 이벤트 발행
        eventPublisher.publishEvent(new ArticleDeleteEvent(articleId));
    }

    /**
     * 키워드로 (게시판 구분 없이) 전체 게시글 검색.
     *  1) Elasticsearch 로 keyword 에 매칭되는 게시글 id 목록을 가져온다. (페이징 X)
     *  2) 그 id 들로 MySQL 을 조회하면서, 이때 페이징(pageable)을 적용해 한 페이지만 반환한다.
     *
     * 정렬/페이징은 MySQL 단계에서 pageable 기준(기본: createdAt 내림차순)으로 처리한다.
     * 조회만 하므로 readOnly.
     */
    @Transactional(readOnly = true)
    public List<Article> searchArticles(String keyword, Pageable pageable) {
        List<Long> ids = elasticsearchService.search(keyword);
        if (ids.isEmpty()) {
            return List.of();
        }

        // id 들 중에서 pageable(페이지 번호/크기/정렬)에 맞는 한 페이지만 MySQL 에서 조회
        return articleRepository.findByIdIn(ids, pageable);
    }
}
