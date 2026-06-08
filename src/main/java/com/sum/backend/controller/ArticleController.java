package com.sum.backend.controller;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
