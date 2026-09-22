package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 站内通知 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO implements Serializable {

    private Long id;
    private String type;
    private String title;
    private String body;
    /** CONTENT / CREATOR_APPLICATION，配合 bizId 让前端能跳过去 */
    private String bizType;
    private Long bizId;
    /** true 已读 */
    private Boolean read;
    private LocalDateTime createTime;
}
