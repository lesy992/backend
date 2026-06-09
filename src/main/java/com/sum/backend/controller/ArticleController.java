package com.sum.backend.controller;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.dto.EditArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.service.ArticleService;
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


@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/articles")
    public ResponseEntity<Article> createArticle(
            @RequestBody @Valid CreateArticle request,
            @AuthenticationPrincipal String loginId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.writeArticle(request, loginId));
    }

    @GetMapping("/{boardId}/articles")
    public List<Article> getArticlesByBoard(
            @PathVariable Long boardId,
            @RequestParam(required = false) Long lastId, @RequestParam(required = false) Long firstId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if(lastId != null){
            return articleService.getArticlesForOld(boardId, lastId, pageable);
        }
        if(firstId != null){
            return articleService.getArticlesForNew(boardId, firstId, pageable);
        }
        return articleService.getArticles(boardId, pageable);
    }

    @PutMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Article> editArticles(
            @PathVariable Long boardId, @PathVariable Long articleId,
            @RequestBody EditArticle editArticle, @AuthenticationPrincipal String loginId) {
        return ResponseEntity.ok(articleService.editArticle(boardId, articleId, editArticle, loginId));
    }

    @DeleteMapping("/{boardId}/articles/{articleId}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable Long boardId, @PathVariable Long articleId,
            @AuthenticationPrincipal String loginId) {
        articleService.deleteArticle(boardId, articleId, loginId);
        return ResponseEntity.noContent().build();
    }
}
