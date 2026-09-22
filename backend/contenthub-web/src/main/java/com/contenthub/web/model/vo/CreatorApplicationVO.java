package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 创作者申请 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorApplicationVO implements Serializable {

    private Long id;
    private Long userId;
    /** 申请人用户名，管理端列表展示用 */
    private String username;
    private String nickname;
    private String intro;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String rejectReason;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
