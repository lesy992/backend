package com.sum.backend.dto;

import lombok.Getter;

@Getter
public class CreateArticle {

    private Long boardId;

    private String title;

    private String content;

}
