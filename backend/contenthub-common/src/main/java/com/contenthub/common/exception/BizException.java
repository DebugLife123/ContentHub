package com.contenthub.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @author newone
 * @date 2023/11/14
 * @description 业务异常
 */
@Getter
@Setter
public class BizException extends RuntimeException {
    //异常码
    private String errorCode;
    //错误信息
    private String errorMessage;

    public BizException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMessage = baseExceptionInterface.getErrorMessage();
    }

    /**
     * 需要拼接动态提示时的构造函数，例如把校验规则写进消息里。
     */
    public BizException(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
