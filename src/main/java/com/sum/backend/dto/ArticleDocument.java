package com.sum.backend.dto;


import com.sum.backend.entity.Article;
import lombok.Builder;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class ArticleDocument {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Long boardId;
    private Long viewCount;
    private String createdAt; // 예: "2026-06-18T17:36:00"
    private String updatedAt;

    public static ArticleDocument fromEntity(Article article) {
        return ArticleDocument.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .authorId(article.getAuthor() != null ? article.getAuthor().getId() : null)
                .boardId(article.getBoard() != null ? article.getBoard().getId() : null)
                .viewCount(article.getViewCount())
                .createdAt(article.getCreatedAt() != null ? article.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .updatedAt(article.getUpdatedAt() != null ? article.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}