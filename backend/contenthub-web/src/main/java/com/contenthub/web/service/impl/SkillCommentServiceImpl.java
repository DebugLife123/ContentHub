package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.SkillCommentDO;
import com.contenthub.common.domain.dos.SkillDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.SkillCommentMapper;
import com.contenthub.common.domain.mapper.SkillMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.SkillCommentVO;
import com.contenthub.web.service.SkillCommentService;
import com.contenthub.web.util.CurrentUserUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillCommentServiceImpl implements SkillCommentService {

    private static final String NORMAL = "NORMAL";
    private static final String HIDDEN = "HIDDEN";
    private static final int MAX_BODY = 1000;

    private final SkillCommentMapper commentMapper;
    private final SkillMapper skillMapper;
    private final UserMapper userMapper;

    public SkillCommentServiceImpl(SkillCommentMapper commentMapper,
                                   SkillMapper skillMapper,
                                   UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Response<PageResponse<SkillCommentVO>> listBySkill(Long skillId, long pageNum, long pageSize) {
        // 公开列表只给正常状态，隐藏的只有管理端能看到
        Page<SkillCommentDO> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SkillCommentDO>()
                        .eq(SkillCommentDO::getSkillId, skillId)
                        .eq(SkillCommentDO::getStatus, NORMAL)
                        .orderByDesc(SkillCommentDO::getCreateTime)
                        .orderByDesc(SkillCommentDO::getId));

        Map<Long, String> usernames = usernames(page.getRecords());
        Map<Long, String> skillNames = skillNames(page.getRecords());
        return Response.success(PageResponse.of(page, c -> toVO(c, usernames, skillNames)));
    }

    @Override
    @Transactional
    public Response<SkillCommentVO> create(Long skillId, String body) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        if (StringUtils.isBlank(body)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "评论内容不能为空");
        }
        if (body.length() > MAX_BODY) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(),
                    "评论内容不能超过 " + MAX_BODY + " 个字符");
        }

        SkillDO skill = skillMapper.selectById(skillId);
        // 只允许给已上架的 Skill 评论，草稿/已下架的评不了
        if (skill == null || !"PUBLISHED".equals(skill.getStatus())) {
            throw new BizException(ResponseCodeEnum.SKILL_NOT_FOUND);
        }

        SkillCommentDO comment = SkillCommentDO.builder()
                .skillId(skillId)
                .userId(loginUser.getUserId())
                .body(body.trim())
                .status(NORMAL)
                .build();
        commentMapper.insert(comment);

        // 回填后重新查一次，拿到数据库生成的 create_time
        SkillCommentDO saved = commentMapper.selectById(comment.getId());
        Map<Long, String> usernames = Map.of(loginUser.getUserId(), loginUser.getUsername());
        Map<Long, String> skillNames = Map.of(skillId, skill.getName());
        return Response.success(toVO(saved, usernames, skillNames));
    }

    @Override
    public Response<PageResponse<SkillCommentVO>> myComments(long pageNum, long pageSize) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        Page<SkillCommentDO> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SkillCommentDO>()
                        .eq(SkillCommentDO::getUserId, loginUser.getUserId())
                        .orderByDesc(SkillCommentDO::getCreateTime)
                        .orderByDesc(SkillCommentDO::getId));

        Map<Long, String> usernames = Map.of(loginUser.getUserId(), loginUser.getUsername());
        Map<Long, String> skillNames = skillNames(page.getRecords());
        return Response.success(PageResponse.of(page, c -> toVO(c, usernames, skillNames)));
    }

    @Override
    @Transactional
    public Response<Void> delete(Long commentId) {
        SkillCommentDO comment = requireExisting(commentId);
        requireOwnerOrAdmin(comment);
        commentMapper.deleteById(commentId);
        return Response.success();
    }

    // ------------------------------------------------------------------ 管理端

    @Override
    public Response<PageResponse<SkillCommentVO>> pageForAdmin(long pageNum, long pageSize, String status) {
        LambdaQueryWrapper<SkillCommentDO> query = new LambdaQueryWrapper<SkillCommentDO>()
                .orderByDesc(SkillCommentDO::getCreateTime)
                .orderByDesc(SkillCommentDO::getId);
        if (StringUtils.isNotBlank(status)) {
            query.eq(SkillCommentDO::getStatus, status.trim().toUpperCase());
        }

        Page<SkillCommentDO> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), query);
        Map<Long, String> usernames = usernames(page.getRecords());
        Map<Long, String> skillNames = skillNames(page.getRecords());
        return Response.success(PageResponse.of(page, c -> toVO(c, usernames, skillNames)));
    }

    @Override
    @Transactional
    public Response<Void> updateStatus(Long commentId, String status) {
        requireExisting(commentId);
        String target = StringUtils.isBlank(status) ? NORMAL : status.trim().toUpperCase();
        if (!NORMAL.equals(target) && !HIDDEN.equals(target)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(),
                    "状态只能是 NORMAL 或 HIDDEN");
        }
        commentMapper.update(null, new LambdaUpdateWrapper<SkillCommentDO>()
                .eq(SkillCommentDO::getId, commentId)
                .set(SkillCommentDO::getStatus, target));
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部

    private SkillCommentDO requireExisting(Long commentId) {
        SkillCommentDO comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ResponseCodeEnum.SKILL_COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void requireOwnerOrAdmin(SkillCommentDO comment) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if ("ADMIN".equals(loginUser.getRole())) {
            return;
        }
        if (!Objects.equals(comment.getUserId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.NOT_SKILL_COMMENT_OWNER);
        }
    }

    /** 当前登录用户可删的条件：自己发的，或者管理员 */
    private boolean canDelete(SkillCommentDO c) {
        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return "ADMIN".equals(loginUser.getRole()) || Objects.equals(c.getUserId(), loginUser.getUserId());
    }

    private Map<Long, String> usernames(List<SkillCommentDO> comments) {
        Set<Long> ids = comments.stream().map(SkillCommentDO::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (UserDO user : userMapper.selectBatchIds(ids)) {
            names.put(user.getId(),
                    StringUtils.defaultIfBlank(user.getNickname(), user.getUsername()));
        }
        return names;
    }

    private Map<Long, String> skillNames(List<SkillCommentDO> comments) {
        Set<Long> ids = comments.stream().map(SkillCommentDO::getSkillId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (SkillDO skill : skillMapper.selectBatchIds(ids)) {
            names.put(skill.getId(), skill.getName());
        }
        return names;
    }

    private SkillCommentVO toVO(SkillCommentDO c, Map<Long, String> usernames, Map<Long, String> skillNames) {
        return SkillCommentVO.builder()
                .id(c.getId())
                .skillId(c.getSkillId())
                .skillName(skillNames.get(c.getSkillId()))
                .userId(c.getUserId())
                .username(usernames.get(c.getUserId()))
                .status(c.getStatus())
                .body(c.getBody())
                .canDelete(canDelete(c))
                .createTime(c.getCreateTime())
                .build();
    }
}
