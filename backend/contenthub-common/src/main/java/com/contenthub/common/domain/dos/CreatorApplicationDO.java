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
 * 创作者申请（对应 creator_application 表）。
 *
 * <p>改造前 {@code POST /creator/apply} 是直接把 {@code users.role} 改成 CREATOR，
 * 任何注册用户一键就能发布内容，「创作者」这道门槛形同虚设。
 * 现在改成提交申请 → 管理员审核 → 通过才升级角色。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("creator_application")
public class CreatorApplicationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 申请说明 */
    private String intro;

    /** PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回 */
    private String status;

    /** 驳回原因，仅 REJECTED 时有值 */
    private String rejectReason;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
