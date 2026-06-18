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
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 댓글(Comment) 관련 실제 비즈니스 로직을 담당하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    /**
     * 댓글/대댓글 작성.
     * 1) 작성자 조회
     * 2) 댓글을 달 게시글 조회 + 게시판 일치 검증
     * 3) parentId 가 있으면 부모 댓글을 찾고, "대댓글의 대댓글"은 막는다(답글은 1단계까지만 허용)
     * 4) 댓글 저장 후 응답 DTO로 변환해 반환
     */
    @Transactional
    public CommentResponse createComment(Long boardId, Long articleId, CreateComment request, String loginId) {
        User author = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }

        // parentId 가 있으면 대댓글, 없으면 일반 댓글(parent = null)
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

    /**
     * 특정 게시글의 댓글 목록 조회. (조회 전용이라 readOnly)
     * findTopLevelCommentsByArticleId : 부모가 없는 "최상위 댓글"만 조회.
     *   (대댓글은 CommentResponse 안에서 각 부모 댓글의 children 으로 포함되어 내려감)
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long boardId, Long articleId) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!article.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속한 게시글이 아닙니다.");
        }
        return commentRepository.findTopLevelCommentsByArticleId(articleId).stream()
                .map(CommentResponse::new) // 엔티티 → 응답 DTO 변환
                .toList();
    }

    /**
     * 댓글 수정. 본인이 쓴 댓글만 수정 가능.
     * 검증: 요청자 확인 → 게시글/게시판 확인 → 댓글 확인 → 그 댓글이 이 게시글 소속인지 → 작성자 본인인지.
     * comment.update(...) 로 값만 바꿔두면 트랜잭션 종료 시 자동 UPDATE.
     */
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
