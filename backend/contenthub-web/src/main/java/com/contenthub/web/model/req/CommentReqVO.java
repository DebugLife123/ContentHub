package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 发表评论（阶段 5 Day 45） */
@Data
public class CommentReqVO implements Serializable {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论不能超过 1000 个字符")
    private String body;
}
