package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.CreatorApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface CreatorApplicationMapper extends BaseMapper<CreatorApplicationDO> {

    /**
     * 带「仍处于待审核」条件的审核落库。
     *
     * <p>两个管理员同时对同一条申请点「通过」和「驳回」时，若只按 id 更新，
     * 两次都会成功：角色可能已经升为 CREATOR（approve 里的副作用），
     * 申请记录却被后到的 reject 覆盖成 REJECTED，状态自相矛盾，
     * 申请人还会同时收到两条相反的通知。</p>
     *
     * @return 影响行数；0 表示这条申请已被别人处理过
     */
    @Update("UPDATE creator_application SET status = #{target}, reject_reason = #{rejectReason}, "
            + "reviewer_id = #{reviewerId}, review_time = #{reviewTime} "
            + "WHERE id = #{id} AND status = 'PENDING' AND is_deleted = 0")
    int review(@Param("id") Long id,
               @Param("target") String target,
               @Param("rejectReason") String rejectReason,
               @Param("reviewerId") Long reviewerId,
               @Param("reviewTime") LocalDateTime reviewTime);
}
