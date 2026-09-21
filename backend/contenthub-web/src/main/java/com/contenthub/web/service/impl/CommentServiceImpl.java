package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.CommentDO;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.CommentMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.CommentReqVO;
import com.contenthub.web.model.vo.CommentVO;
import com.contenthub.web.service.CommentService;
import com.contenthub.web.util.CurrentUserUtil;
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
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(CommentMapper commentMapper, ContentMapper contentMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.contentMapper = contentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Response<PageResponse<CommentVO>> listByContent(Long contentId, long pageNum, long pageSize) {
        ContentDO content = contentMapper.selectById(contentId);
        if (Objects.isNull(content) || !"PUBLISHED".equals(content.getStatus())) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }

        Page<CommentDO> page = commentMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<CommentDO>()
                        .eq(CommentDO::getContentId, contentId)
                        .eq(CommentDO::getStatus, "NORMAL")
                        .orderByDesc(CommentDO::getId));

        Map<Long, String> usernames = usernames(page.getRecords().stream()
                .map(CommentDO::getUserId).collect(Collectors.toSet()));

        return Response.success(PageResponse.of(page,
                c -> toVO(c, null, usernames.get(c.getUserId()))));
    }

    @Override
    @Transactional
    public Response<CommentVO> create(Long contentId, CommentReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        ContentDO content = contentMapper.selectById(contentId);
        if (Objects.isNull(content) || !"PUBLISHED".equals(content.getStatus())) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }

        CommentDO comment = CommentDO.builder()
                .contentId(contentId)
                .userId(loginUser.getUserId())
                .body(req.getBody().trim())
                .status("NORMAL")
                .build();
        commentMapper.insert(comment);

        return Response.success(toVO(comment, content.getTitle(), loginUser.getUsername()));
    }

    @Override
    @Transactional
    public Response<Void> delete(Long commentId) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        CommentDO comment = commentMapper.selectById(commentId);
        if (Objects.isNull(comment)) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 本人或管理员可删；其余人一律拒绝
        boolean isAdmin = "ADMIN".equals(loginUser.getRole());
        if (!isAdmin && !Objects.equals(comment.getUserId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.NOT_COMMENT_OWNER);
        }

        commentMapper.deleteById(commentId);
        return Response.success();
    }

    @Override
    public Response<PageResponse<CommentVO>> myComments(long pageNum, long pageSize) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        Page<CommentDO> page = commentMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<CommentDO>()
                        .eq(CommentDO::getUserId, loginUser.getUserId())
                        .orderByDesc(CommentDO::getId));

        Map<Long, String> titles = contentTitles(page.getRecords().stream()
                .map(CommentDO::getContentId).collect(Collectors.toSet()));

        return Response.success(PageResponse.of(page,
                c -> toVO(c, titles.get(c.getContentId()), loginUser.getUsername())));
    }

    @Override
    public long countByContent(Long contentId) {
        Long count = commentMapper.selectCount(new LambdaQueryWrapper<CommentDO>()
                .eq(CommentDO::getContentId, contentId)
                .eq(CommentDO::getStatus, "NORMAL"));
        return count == null ? 0L : count;
    }

    @Override
    public Response<PageResponse<CommentVO>> adminList(String status, long pageNum, long pageSize) {
        LambdaQueryWrapper<CommentDO> query = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            query.eq(CommentDO::getStatus, status.trim().toUpperCase());
        }
        query.orderByDesc(CommentDO::getId);

        Page<CommentDO> page = commentMapper.selectPage(Page.of(pageNum, pageSize), query);

        Map<Long, String> usernames = usernames(page.getRecords().stream()
                .map(CommentDO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> titles = contentTitles(page.getRecords().stream()
                .map(CommentDO::getContentId).collect(Collectors.toSet()));

        return Response.success(PageResponse.of(page,
                c -> toVO(c, titles.get(c.getContentId()), usernames.get(c.getUserId()))));
    }

    @Override
    @Transactional
    public Response<Void> setStatus(Long commentId, String status) {
        String normalized = StringUtils.defaultIfBlank(status, "").trim().toUpperCase();
        if (!Set.of("NORMAL", "HIDDEN").contains(normalized)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(),
                    "状态只能是 NORMAL 或 HIDDEN");
        }

        CommentDO comment = commentMapper.selectById(commentId);
        if (Objects.isNull(comment)) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }

        commentMapper.updateById(CommentDO.builder().id(commentId).status(normalized).build());
        return Response.success();
    }

    // ------------------------------------------------------------------ 内部方法

    private CommentVO toVO(CommentDO c, String contentTitle, String username) {
        LoginUser loginUser = CurrentUserUtil.getLoginUser();
        boolean canDelete = loginUser != null
                && ("ADMIN".equals(loginUser.getRole()) || Objects.equals(c.getUserId(), loginUser.getUserId()));

        return CommentVO.builder()
                .id(c.getId())
                .contentId(c.getContentId())
                .contentTitle(contentTitle)
                .userId(c.getUserId())
                .username(username)
                .status(c.getStatus())
                .body(c.getBody())
                .canDelete(canDelete)
                .createTime(c.getCreateTime())
                .build();
    }

    private Map<Long, String> usernames(Set<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(UserDO::getId,
                        u -> StringUtils.defaultIfBlank(u.getNickname(), u.getUsername()), (a, b) -> a));
    }

    private Map<Long, String> contentTitles(Set<Long> contentIds) {
        List<Long> ids = contentIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return contentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ContentDO::getId, ContentDO::getTitle, (a, b) -> a));
    }
}
