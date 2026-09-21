package com.contenthub.web.util;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.jwt.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 中取当前登录用户。
 *
 * <p>{@link LoginUser} 里带了 userId 与 role，所以判断内容归属、角色时不必再回查数据库。</p>
 */
public final class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    /** 未登录时返回 null */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 需要登录的场景：未登录直接抛 401 */
    public static LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }
        return loginUser;
    }

    /** 是否管理员 */
    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && "ADMIN".equals(loginUser.getRole());
    }
}
