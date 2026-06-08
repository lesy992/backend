package com.sum.backend.service;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.entity.Board;
import com.sum.backend.entity.User;
import com.sum.backend.repository.ArticleRepository;
import com.sum.backend.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    private final BoardRepository boardRepository;

    @Transactional
    public Article witeArticle(CreateArticle article, User author){

        Optional<Board> board = boardRepository.findById(article.getBoardId());

        Article newArticle = Article.builder()
                .author(author)
                .board(board.get())
                .title(article.getTitle())
                .content(article.getContent())
                .build();

        // DB에 저장 후, 영속화된 엔티티 반환
        return articleRepository.save(newArticle);
    }
}
