package com.sum.backend.service;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.entity.Board;
import com.sum.backend.entity.User;
import com.sum.backend.repository.ArticleRepository;
import com.sum.backend.repository.BoardRepository;
import com.sum.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

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
                .build();

        return articleRepository.save(newArticle);
    }
}
