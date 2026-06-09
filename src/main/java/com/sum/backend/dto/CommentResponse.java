package com.sum.backend.dto;

import com.sum.backend.entity.Comment;
import com.sum.backend.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentResponse {

    private final Long id;
    private final String content;
    private final User author;
    private final Long parentId;
    private final List<CommentResponse> replies;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = comment.getAuthor();
        this.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        this.replies = comment.getReplies().stream()
                .map(CommentResponse::new)
                .toList();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }
}
