package com.sum.backend.controller;

import com.sum.backend.dto.CommentResponse;
import com.sum.backend.dto.CreateComment;
import com.sum.backend.dto.EditComment;
import com.sum.backend.entity.Article;
import com.sum.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 댓글(Comment) 관련 HTTP 요청을 받는 컨트롤러.
 * 댓글은 "특정 게시판의 특정 게시글"에 속하므로 경로가
 *   /api/boards/{boardId}/articles/{articleId}/comments ... 형태
 */
@Tag(name = "댓글(Comment)", description = "댓글/대댓글 작성·조회·수정 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글(또는 대댓글) 작성.
     * 대댓글이면 요청 본문(CreateComment)에 parentId 를 담아 보냄.
     * 성공 시 201 Created.
     */
    @Operation(
            summary = "댓글/대댓글 작성",
            description = "특정 게시글에 댓글을 작성, 요청 본문의 parentId 가 있으면 해당 댓글의 대댓글로 등록되며, 대댓글에는 다시 답글을 달 수 없음(1단계만 허용).")
    @PostMapping("/{boardId}/articles/{articleId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId,
            @RequestBody @Valid CreateComment request,
            @Parameter(hidden = true) @AuthenticationPrincipal String loginId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(boardId, articleId, request, loginId));
    }

    /**
     * 특정 게시글의 댓글 목록 조회. (대댓글은 각 부모 댓글 안에 포함되어 내려감)
     */
    @Operation(
            summary = "댓글 목록 조회",
            description = "특정 게시글의 최상위 댓글 목록을 조회, 각 댓글의 대댓글은 children 형태로 함께 내려감")
    @GetMapping("/{boardId}/articles/{articleId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getComments(boardId, articleId));
    }

    /**
     * 댓글 수정. 본인이 쓴 댓글만 수정 가능. (권한 검증은 서비스에서)
     */
    @Operation(
            summary = "댓글 수정",
            description = "본인이 작성한 댓글의 내용을 수정. 작성자가 아니거나 해당 게시글의 댓글이 아니면 예외가 발생")
    @PutMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> editComment(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @RequestBody @Valid EditComment request,
            @Parameter(hidden = true) @AuthenticationPrincipal String loginId) {
        return ResponseEntity.ok(commentService.editComment(boardId, articleId, commentId, request, loginId));
    }

    /**
     * 댓글 삭제. 본인이 쓴 댓글만 삭제 가능. 부모 댓글을 지우면 대댓글도 함께 삭제된다.
     */
    @Operation(
            summary = "댓글 삭제",
            description = "본인이 작성한 댓글을 삭제. 부모 댓글을 삭제하면 대댓글도 함께 삭제되며, 성공 시 204 No Content 를 반환")
    @DeleteMapping("/{boardId}/articles/{articleId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID") @PathVariable Long articleId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal String loginId) {
        commentService.deleteComment(boardId, articleId, commentId, loginId);
        return ResponseEntity.noContent().build();
    }

}
