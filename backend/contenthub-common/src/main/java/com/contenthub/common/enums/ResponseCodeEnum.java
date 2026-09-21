package com.contenthub.common.enums;

import com.contenthub.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author newone
 * @date 2023/11/14
 * @description 响应异常码
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),
    // ----------- 业务异常状态码 -----------
    //PRODUCT_NOT_FOUND("20000", "该产品不存在（测试使用）"),
    LOGIN_FAIL("20000", "登录失败"),
    USERNAME_OR_PWD_ERROR("20001", "用户名或密码错误"),
    UNAUTHORIZED("20002", "无访问权限，请先登录！"),
    FORBIDDEN("20004", "权限不足，无法访问该资源"),
    USERNAME_EXISTS("20005", "该用户名已被注册"),
    CONTENT_NOT_FOUND("20006", "内容不存在或尚未发布"),
    CATEGORY_NOT_FOUND("20007", "分类不存在"),
    CATEGORY_NAME_EXISTS("20008", "该分类名称已存在"),
    NOT_CONTENT_OWNER("20009", "只能操作自己发布的内容"),
    CATEGORY_IN_USE("20010", "该分类下还有内容，无法删除"),
    CONTENT_STATUS_ILLEGAL("20011", "当前状态不允许该操作"),
    ALREADY_FAVORITED("20012", "已经收藏过了"),
    NOT_FAVORITED("20013", "尚未收藏该内容"),
    PLAN_NOT_FOUND("20014", "订阅套餐不存在或已下架"),
    PLAN_IN_USE("20015", "该套餐已有订阅记录，无法删除"),
    NOT_CREATOR("20017", "还不是创作者，请先申请创作者身份"),
    SUBSCRIPTION_NOT_FOUND("20018", "订阅记录不存在"),
    ACCOUNT_DISABLED("20019", "账号已被禁用，请联系管理员"),
    COMMENT_NOT_FOUND("20020", "评论不存在"),
    NOT_COMMENT_OWNER("20021", "只能删除自己的评论"),
    SELECT_FAIL("20003","查询数据库时出错");
    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

}