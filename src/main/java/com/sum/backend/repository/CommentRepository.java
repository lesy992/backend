package com.sum.backend.repository;

import com.sum.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만 조회 (대댓글 포함해서 replies로 접근)
    @Query("SELECT c FROM Comment c WHERE c.article.id = :articleId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByArticleId(@Param("articleId") Long articleId);

    void deleteByArticleId(Long articleId);
}
