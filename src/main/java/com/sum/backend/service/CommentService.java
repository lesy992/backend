package com.sum.backend.service;

import com.sum.backend.dto.CommentResponse;
import com.sum.backend.dto.CreateComment;
import com.sum.backend.dto.EditComment;
import com.sum.backend.entity.Article;
import com.sum.backend.entity.Comment;
import com.sum.backend.entity.User;
import com.sum.backend.repository.ArticleRepository;
import com.sum.backend.repository.CommentRepository;
import com.sum.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long boardId, Long articleId, CreateComment request, String loginId) {
        User author = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
            // 대댓글에 대한 대댓글 방지 (1단계만 허용)
            if (parent.getParent() != null) {
                throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다.");
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .author(author)
                .article(article)
                .parent(parent)
                .build();

        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long boardId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        return commentRepository.findTopLevelCommentsByArticleId(articleId).stream()
                .map(CommentResponse::new)
                .toList();
    }

    @Transactional
    public CommentResponse editComment(Long boardId, Long articleId, Long commentId, EditComment request, String loginId) {
        User requestUser = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (!comment.getArticle().getId().equals(articleId)) {
            throw new IllegalArgumentException("해당 게시글에 속한 댓글이 아닙니다.");
        }

        if (!comment.getAuthor().getId().equals(requestUser.getId())) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }

        comment.update(request.getContent());
        return new CommentResponse(comment);
    }
}
