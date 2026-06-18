package com.sum.backend.repository;

import com.sum.backend.entity.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 DB 접근 계층(Repository).
 * JpaRepository 를 상속하면 save / findById / findAll / delete 같은 기본 CRUD 메서드가 자동 제공된다.
 * 아래는 기본 제공되지 않는 "커스텀 조회/수정" 메서드들.
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>{

    // 메서드 이름만으로 쿼리가 자동 생성됨(Query Method).
    // findByBoardId → "board_id 컬럼이 일치하는 게시글들을 조회" + Pageable 로 페이징 처리.
    List<Article> findByBoardId(Long boardId, Pageable pageable);

    // ES 검색으로 얻은 id 목록에 해당하는 게시글들을, pageable(페이지/정렬) 기준으로 조회.
    // findByIdIn → "id 컬럼이 주어진 목록(IN) 안에 있는 게시글들을 조회"
    List<Article> findByIdIn(List<Long> ids, Pageable pageable);

    @Query(value = "SELECT a FROM Article a " +
            "WHERE a.board.id = :boardId AND a.id < :articleId " +
            "ORDER BY a.createdAt DESC")
        // LIMIT 10을 JPQL 내에 직접 쓸 수 없으므로, Pageable 객체를 파라미터로 넘겨 처리
    List<Article> findArticlesForOld(@Param("boardId") Long boardId,
                                        @Param("articleId") Long articleId,
                                        Pageable pageable);

    @Query(value = "SELECT a FROM Article a " +
            "WHERE a.board.id = :boardId AND a.id > :articleId " +
            "ORDER BY a.createdAt DESC")
        // LIMIT 10을 JPQL 내에 직접 쓸 수 없으므로, Pageable 객체를 파라미터로 넘겨 처리
    List<Article> findArticlesForNew(@Param("boardId") Long boardId,
                                        @Param("articleId") Long articleId,
                                        Pageable pageable);

    // 조회수 +1 처리.
    // @Modifying : 조회(SELECT)가 아니라 변경(UPDATE/DELETE) 쿼리임 (없으면 실행 에러)
    // "viewCount = viewCount + 1" 처럼 DB에서 직접 증가시키므로,
    //   "읽어와서 +1 해서 저장" 방식과 달리 동시에 여러 명이 조회해도 값이 누락되지 않음(원자적 처리).
    // clearAutomatically = true : UPDATE 후 영속성 컨텍스트(1차 캐시)를 비움
    //   → 이후 findById 로 다시 조회하면 증가된 최신 조회수를 DB에서 가져옴
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int increaseViewCount(@Param("id") Long id);

}
