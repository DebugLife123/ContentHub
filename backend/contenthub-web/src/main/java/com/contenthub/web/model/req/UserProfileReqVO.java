package com.contenthub.web.model.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 修改个人资料（用户名与角色不可改） */
@Data
public class UserProfileReqVO implements Serializable {

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    @Size(max = 300, message = "个人简介不能超过 300 个字符")
    private String bio;
}
