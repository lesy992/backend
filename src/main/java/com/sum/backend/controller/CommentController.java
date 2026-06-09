package com.sum.backend.controller;

import com.sum.backend.dto.CommentResponse;
import com.sum.backend.dto.CreateComment;
import com.sum.backend.dto.EditComment;
import com.sum.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/articles/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long boardId,
            @PathVariable Long articleId,
            @RequestBody @Valid CreateComment request,
            @AuthenticationPrincipal String loginId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(boardId, articleId, request, loginId));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getComments(boardId, articleId));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> editComment(
            @PathVariable Long boardId,
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            @RequestBody @Valid EditComment request,
            @AuthenticationPrincipal String loginId) {
        return ResponseEntity.ok(commentService.editComment(boardId, articleId, commentId, request, loginId));
    }
}
