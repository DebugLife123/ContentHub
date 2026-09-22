package com.contenthub.common.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内通知（对应 notification 表）。
 *
 * <p>补的是一个「反馈回路」：改造前管理员驳回内容会把原因写进
 * {@code contents.reject_reason}，但创作者不主动去工作台翻就永远不知道。
 * 现在审核、上架下架、创作者申请等动作都会写一条通知给当事人。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class NotificationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    /** CONTENT_APPROVED / CONTENT_REJECTED / CONTENT_OFFLINE / CREATOR_APPROVED / CREATOR_REJECTED */
    private String type;

    private String title;

    private String body;

    /** CONTENT / CREATOR_APPLICATION */
    private String bizType;

    private Long bizId;

    /** 0 未读 / 1 已读 */
    private Integer readFlag;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
