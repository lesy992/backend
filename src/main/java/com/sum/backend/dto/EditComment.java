package com.sum.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EditComment {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;
}
