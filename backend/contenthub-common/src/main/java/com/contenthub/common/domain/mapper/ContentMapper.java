package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.ContentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper extends BaseMapper<ContentDO> {

    /**
     * 带「当前状态」条件的状态流转。
     *
     * <p>{@code WHERE status = #{current}} 是并发保护：两个管理员同时对同一条待审内容
     * 点「通过」和「驳回」时，先到的那条把状态改掉，后到的这条匹配不到行、影响行数为 0。
     * 如果只按 id 更新，两边都会"成功"，作者会同时收到「已发布」和「已驳回」两条矛盾通知。</p>
     *
     * <p>驳回原因在同一条语句里更新：{@code rejectReason} 传 null 即清空，
     * 顺便解决了「updateById 忽略 null、清不掉旧驳回原因」的问题，
     * 因此不需要额外的 clearRejectReason。</p>
     *
     * @return 影响行数；0 表示状态已被别人改变，本次流转失败
     */
    @Update("UPDATE contents SET status = #{target}, reject_reason = #{rejectReason} "
            + "WHERE id = #{id} AND status = #{current} AND is_deleted = 0")
    int transitionStatus(@Param("id") Long id,
                         @Param("current") String current,
                         @Param("target") String target,
                         @Param("rejectReason") String rejectReason);
}
