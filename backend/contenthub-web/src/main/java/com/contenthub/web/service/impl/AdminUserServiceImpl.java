package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.AdminUserVO;
import com.contenthub.web.service.AdminUserService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final ContentMapper contentMapper;

    public AdminUserServiceImpl(UserMapper userMapper, ContentMapper contentMapper) {
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
    }

    @Override
    public Response<PageResponse<AdminUserVO>> page(String keyword, String role, long pageNum, long pageSize) {
        LambdaQueryWrapper<UserDO> query = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            query.and(w -> w.like(UserDO::getUsername, kw).or().like(UserDO::getNickname, kw));
        }
        if (StringUtils.isNotBlank(role)) {
            query.eq(UserDO::getRole, role.trim().toUpperCase());
        }
        query.orderByDesc(UserDO::getId);

        Page<UserDO> page = userMapper.selectPage(Page.of(pageNum, pageSize), query);

        Map<Long, Long> contentCounts = contentCounts(page.getRecords().stream()
                .map(UserDO::getId).collect(Collectors.toSet()));

        return Response.success(PageResponse.of(page, u -> AdminUserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus())
                .contentCount(contentCounts.getOrDefault(u.getId(), 0L))
                .createTime(u.getCreateTime())
                .build()));
    }

    @Override
    @Transactional
    public Response<Void> updateStatus(Long userId, String status) {
        String normalized = StringUtils.defaultIfBlank(status, "").trim().toUpperCase();
        if (!Set.of("ENABLED", "DISABLED").contains(normalized)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(),
                    "状态只能是 ENABLED 或 DISABLED");
        }

        UserDO user = userMapper.selectById(userId);
        if (Objects.isNull(user)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "用户不存在");
        }

        // 不允许管理员把自己禁用掉，否则会把自己锁在门外
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if (Objects.equals(userId, loginUser.getUserId()) && "DISABLED".equals(normalized)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "不能禁用当前登录的管理员账号");
        }

        userMapper.updateById(UserDO.builder().id(userId).status(normalized).build());
        log.info("管理员 {} 将用户 {} 状态改为 {}", loginUser.getUsername(), user.getUsername(), normalized);

        return Response.success();
    }

    private Map<Long, Long> contentCounts(Set<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .select(ContentDO::getCreatorId)
                .in(ContentDO::getCreatorId, ids));
        return contents.stream().collect(Collectors.groupingBy(ContentDO::getCreatorId, Collectors.counting()));
    }
}
