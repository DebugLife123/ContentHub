package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 注册请求体（计划表 19：POST /api/auth/register） */
@Data
public class RegisterReqVO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 60, message = "用户名长度需在 3~60 个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 60, message = "密码长度需在 6~60 个字符之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
