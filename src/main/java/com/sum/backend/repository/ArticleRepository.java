package com.sum.backend.repository;

import com.sum.backend.entity.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>{

    List<Article> findByBoardId(Long boardId, Pageable pageable);

    @Query(value = "SELECT a FROM Article a " +
            "WHERE a.board.id = :boardId AND a.id < :articleId " +
            "ORDER BY a.createdAt DESC")
        // LIMIT 10을 JPQL 내에 직접 쓸 수 없으므로, Pageable 객체를 파라미터로 넘겨 처리합니다.
    List<Article> findArticlesForOld(@Param("boardId") Long boardId,
                                        @Param("articleId") Long articleId,
                                        Pageable pageable);

    @Query(value = "SELECT a FROM Article a " +
            "WHERE a.board.id = :boardId AND a.id > :articleId " +
            "ORDER BY a.createdAt DESC")
        // LIMIT 10을 JPQL 내에 직접 쓸 수 없으므로, Pageable 객체를 파라미터로 넘겨 처리합니다.
    List<Article> findArticlesForNew(@Param("boardId") Long boardId,
                                        @Param("articleId") Long articleId,
                                        Pageable pageable);


}
