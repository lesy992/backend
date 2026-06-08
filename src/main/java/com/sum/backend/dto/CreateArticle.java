package com.sum.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateArticle {

    @NotNull(message = "게시판을 선택해주세요.")
    private Long boardId;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하이어야 합니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}
