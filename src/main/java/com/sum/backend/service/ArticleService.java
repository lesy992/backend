package com.sum.backend.service;

import com.sum.backend.dto.CreateArticle;
import com.sum.backend.dto.EditArticle;
import com.sum.backend.entity.Article;
import com.sum.backend.entity.Board;
import com.sum.backend.entity.User;
import com.sum.backend.repository.ArticleRepository;
import com.sum.backend.repository.BoardRepository;
import com.sum.backend.repository.CommentRepository;
import com.sum.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

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

    @Transactional
    public List<Article> getArticles(Long boardId, Pageable pageable) {
        return articleRepository.findByBoardId(boardId, pageable);
    }

    @Transactional
    public List<Article> getArticlesForOld(Long boardId, Long articleId, Pageable pageable) {
        return articleRepository.findArticlesForOld(boardId, articleId, pageable);
    }
    @Transactional
    public List<Article> getArticlesForNew(Long boardId, Long articleId, Pageable pageable) {
        return articleRepository.findArticlesForNew(boardId, articleId, pageable);
    }

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

        article.update(editArticle.getTitle(), editArticle.getContent());

        return article;
    }

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

        commentRepository.deleteByArticleId(articleId);
        articleRepository.delete(article);
    }
}
