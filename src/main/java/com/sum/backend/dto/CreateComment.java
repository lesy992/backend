package com.sum.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateComment {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    private Long parentId; // null이면 일반 댓글, 값이 있으면 대댓글
}
