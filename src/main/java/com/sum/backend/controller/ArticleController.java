package com.sum.backend.controller;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.dto.EditArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 게시글(Article) 관련 HTTP 요청을 받는 컨트롤러.
 * 모든 경로는 "/api/boards" 로 시작 (예: POST /api/boards/articles)
 */
@Tag(name = "게시글(Article)", description = "게시글 작성/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class ArticleController {

    // 실제 비즈니스 로직은 서비스에 위임한다. 컨트롤러는 "요청을 받아 서비스로 넘기고 응답을 만드는" 역할
    private final ArticleService articleService;

    /**
     * 게시글 작성.
     * - @RequestBody @Valid CreateArticle : 요청 본문(JSON)을 DTO로 변환하고 유효성 검사까지 수행
     * - @AuthenticationPrincipal String loginId : 로그인한 사용자의 로그인 ID (JWT 인증에서 꺼내옴)
     * - 성공 시 201 Created 상태코드와 생성된 게시글을 응답
     */
    @Operation(
            summary = "게시글 작성",
            description = "로그인한 사용자가 특정 게시판에 새 게시글을 작성, 성공 시 생성된 게시글과 201 Created 를 반환")
    @PostMapping("/articles")
    public ResponseEntity<Article> createArticle(
            @RequestBody @Valid CreateArticle request,
            @Parameter(hidden = true) @AuthenticationPrincipal String loginId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.writeArticle(request, loginId));
    }

    /**
     * 특정 게시판의 게시글 목록 조회 (무한 스크롤 / 페이징).
     * - lastId 가 있으면 : 그 글보다 "이전(오래된)" 글들을 조회 (아래로 더 보기)
     * - firstId 가 있으면 : 그 글보다 "이후(최신)" 글들을 조회 (위로 새로고침)
     * - 둘 다 없으면 : 최신순 첫 페이지 조회
     * - @PageableDefault : 기본 한 페이지 10개, 생성일(createdAt) 기준 내림차순
     */
    @Operation(
            summary = "게시글 목록 조회",
            description = "특정 게시판의 게시글을 최신순으로 조회 "
                    + "lastId 를 주면 해당 글보다 이전(오래된) 글을, firstId 를 주면 이후(최신) 글을 조회하는 무한 스크롤/커서 페이징을 지원 "
                    + "둘 다 없으면 최신 첫 페이지를 반환")
    @GetMapping("/{boardId}/articles")
    public List<Article> getArticlesByBoard(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "이 ID보다 이전(오래된) 글을 조회할 기준 글 ID") @RequestParam(required = false) Long lastId,
            @Parameter(description = "이 ID보다 이후(최신) 글을 조회할 기준 글 ID") @RequestParam(required = false) Long firstId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if(lastId != null){
            return articleService.getArticlesForOld(boardId, lastId, pageable);
        }
        if(firstId != null){
            return articleService.getArticlesForNew(boardId, firstId, pageable);
        }
        return articleService.getArticles(boardId, pageable);
    }


    /**
     * 전체 게시글 통합 검색 (게시판 구분 없음, 페이징).
     *  - keyword 로 Elasticsearch 전체 검색
     * - @PageableDefault : 기본 한 페이지 10개, 생성일(createdAt) 기준 내림차순
     */
    @Operation(
            summary = "게시글 통합 검색",
            description = "게시판 구분 없이 검색어로 전체 게시글을 조회")
    @GetMapping("/articles/search")
    public List<Article> searchArticles(
            @Parameter(description = "검색어 기준으로 검색") @RequestParam(required = true) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // keyword 로 Elasticsearch 전체 검색 → id 추출 → MySQL 조회 결과 반환
        return articleService.searchArticles(keyword, pageable);
    }

    /**
     * 게시글 1개 상세 조회.
     * 이 API를 호출할 때마다 조회수(viewCount)가 1 증가한다. (실제 증가 처리는 서비스에서)
     */
    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글 1건을 조회, 호출할 때마다 해당 게시글의 조회수(viewCount)가 1 증가")
    @GetMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> getArticle(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId) {
        return ResponseEntity.ok(articleService.getArticle(boardId, articleId));
    }

    /**
     * 게시글 수정.
     * 본인이 쓴 글만 수정 가능하며, 권한 검증은 서비스에서 처리
     */
    @Operation(
            summary = "게시글 수정",
            description = "본인이 작성한 게시글의 제목/내용을 수정 작성자가 아니거나 다른 게시판의 글이면 예외가 발생")
    @PutMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> editArticles(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId,
            @RequestBody EditArticle editArticle,
            @Parameter(hidden = true) @AuthenticationPrincipal String  loginId) {
        return ResponseEntity.ok(articleService.editArticle(boardId, articleId, editArticle, loginId));
    }

    /**
     * 게시글 삭제.
     * 본인 글만 삭제 가능하고, 게시글에 달린 댓글도 함께 삭제된다. (서비스에서 처리)
     * 성공 시 본문 없이 204 No Content 응답.
     */
    @Operation(
            summary = "게시글 삭제",
            description = "본인이 작성한 게시글을 삭제, 게시글에 달린 댓글도 함께 삭제되며, 성공 시 204 No Content 를 반환")
    @DeleteMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Void> deleteArticle(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId,
            @Parameter(hidden = true) @AuthenticationPrincipal String loginId) {
        articleService.deleteArticle(boardId, articleId, loginId);
        return ResponseEntity.noContent().build();
    }
}
