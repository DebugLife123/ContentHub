package com.contenthub.common.exception;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.utils.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Optional;

/**
 * 全局异常处理（阶段 7 Day 56：补齐异常处理与参数校验）。
 *
 * <p><b>状态码约定</b>：HTTP 状态码表达「这次请求在协议层面发生了什么」，
 * 响应体始终是同一个信封（{@code success / errorCode / message / data}）。</p>
 *
 * <table>
 *   <tr><th>情况</th><th>HTTP</th><th>理由</th></tr>
 *   <tr><td>业务规则拒绝（BizException）</td><td>200</td>
 *       <td>请求本身是合法的，只是业务上不允许；前端把 message 就近展示在表单旁</td></tr>
 *   <tr><td>参数校验 / 类型不匹配 / 请求体非法</td><td>400</td>
 *       <td>请求本身有问题，客户端应当修正后再发</td></tr>
 *   <tr><td>认证 / 授权失败</td><td>401 / 403</td><td>由 Spring Security 处理</td></tr>
 *   <tr><td>接口或资源不存在</td><td>404</td><td>路径写错了，需要能一眼看出来</td></tr>
 *   <tr><td>请求方法不支持</td><td>405</td><td>同上</td></tr>
 *   <tr><td>唯一键冲突</td><td>409</td><td>并发下的资源冲突</td></tr>
 *   <tr><td>未预期异常</td><td>500</td><td>服务端 bug，堆栈只进日志</td></tr>
 * </table>
 *
 * <p>早期版本所有异常都返回 200，结果是「路径写错了」和「成功」在状态码上无法区分，
 * Nginx 访问日志与监控里也看不出任何异常。</p>
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 自定义业务异常（含归属校验失败、状态机不允许等）—— 保持 200
     */
    @ExceptionHandler({BizException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("业务异常 {} -> [{}] {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return ResponseEntity.ok(Response.fail(e));
    }

    /**
     * {@code @RequestBody} 上的参数校验失败（@Valid / @Validated）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleMethodArgumentNotValid(HttpServletRequest request,
                                                                        MethodArgumentNotValidException e) {
        return badRequest(request, e.getBindingResult());
    }

    /**
     * 查询参数绑定到对象时的校验失败（例如分页参数的上下限）
     */
    @ExceptionHandler({BindException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleBindException(HttpServletRequest request, BindException e) {
        return badRequest(request, e.getBindingResult());
    }

    /**
     * 方法级参数校验失败（Spring 6.1 起 @RequestParam / @PathVariable 上的约束抛这个）
     */
    @ExceptionHandler({HandlerMethodValidationException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleHandlerMethodValidation(HttpServletRequest request,
                                                                         HandlerMethodValidationException e) {
        String detail = e.getAllErrors().stream()
                .map(err -> Optional.ofNullable(err.getDefaultMessage()).orElse("参数不合法"))
                .reduce((a, b) -> a + "; " + b)
                .orElse(ResponseCodeEnum.PARAM_NOT_VALID.getErrorMessage());
        log.warn("参数校验失败 {} -> {}", request.getRequestURI(), detail);
        return badRequest(ResponseCodeEnum.PARAM_NOT_VALID, detail);
    }

    /**
     * 方法级校验（老写法：@Validated 加在类上）
     */
    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleConstraintViolation(HttpServletRequest request,
                                                                     ConstraintViolationException e) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(violation.getMessage());
        }
        log.warn("参数校验失败 {} -> {}", request.getRequestURI(), sb);
        return badRequest(ResponseCodeEnum.PARAM_NOT_VALID, sb.toString());
    }

    /**
     * 路径变量类型不匹配，例如 {@code GET /contents/abc}
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleTypeMismatch(HttpServletRequest request,
                                                              MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配 {} -> {}={}", request.getRequestURI(), e.getName(), e.getValue());
        return badRequest(ResponseCodeEnum.PARAM_TYPE_MISMATCH,
                String.format("参数 %s 类型不正确", e.getName()));
    }

    /**
     * 请求体不是合法 JSON
     */
    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleNotReadable(HttpServletRequest request,
                                                              HttpMessageNotReadableException e) {
        log.warn("请求体解析失败 {}", request.getRequestURI());
        return badRequest(ResponseCodeEnum.PARAM_NOT_VALID, "请求体格式不正确");
    }

    /**
     * 缺少必填的查询参数
     */
    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleMissingParam(HttpServletRequest request,
                                                               MissingServletRequestParameterException e) {
        log.warn("缺少参数 {} -> {}", request.getRequestURI(), e.getParameterName());
        return badRequest(ResponseCodeEnum.MISSING_PARAM,
                String.format("缺少参数 %s", e.getParameterName()));
    }

    /**
     * Content-Type 不支持。
     *
     * <p>例如用 {@code POST} 打接口却完全不发 Content-Type —— 没有这个处理器时
     * 会落到「未预期异常」返回 500，把一个客户端的用法错误表现成服务端故障。</p>
     */
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleMediaTypeNotSupported(HttpServletRequest request,
                                                                       HttpMediaTypeNotSupportedException e) {
        log.warn("Content-Type 不支持 {} -> {}", request.getRequestURI(), e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Response.fail(ResponseCodeEnum.UNSUPPORTED_MEDIA_TYPE.getErrorCode(),
                        String.format("不支持的请求内容类型 %s，请使用 application/json", e.getContentType())));
    }

    /**
     * 上传文件超过 multipart 限制。
     *
     * <p>这个异常是 Tomcat/Spring 在解析 multipart 阶段抛的，默认行为是直接重置连接，
     * 调用方只会看到一个网络错误（前端表现成「点了没反应」）。配合
     * {@code server.tomcat.max-swallow-size: -1}（让 Tomcat 把剩余请求体读完而不是断开）
     * 才能把话说明白。</p>
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleMaxUploadSize(HttpServletRequest request,
                                                               MaxUploadSizeExceededException e) {
        log.warn("上传超过限制 {} -> {} bytes", request.getRequestURI(), e.getMaxUploadSize());
        // getMaxUploadSize() 在限制来自 spring.servlet.multipart.max-file-size 时会是 -1，
        // 直接换算会得到「超过 0MB」这种自相矛盾的提示，所以这里不给具体数字
        long max = e.getMaxUploadSize();
        String message = max > 0
                ? "文件超过 " + (max / 1024 / 1024) + "MB 限制"
                : "上传的文件超过服务端大小限制";
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Response.fail(ResponseCodeEnum.FILE_TOO_LARGE.getErrorCode(), message));
    }

    /**
     * 请求方法不支持，例如用 GET 调只映射了 POST 的接口
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleMethodNotSupported(HttpServletRequest request,
                                                                     HttpRequestMethodNotSupportedException e) {
        log.warn("方法不支持 {} -> {}", request.getRequestURI(), e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Response.fail(ResponseCodeEnum.METHOD_NOT_ALLOWED.getErrorCode(),
                        String.format("该地址不支持 %s 请求", e.getMethod())));
    }

    /**
     * 路径不存在。
     *
     * <p>Spring Boot 3.2 默认抛 {@code NoResourceFoundException}；
     * {@code NoHandlerFoundException} 仅在开启 throw-exception-if-no-handler-found 时出现，
     * 两个都接住以免漏成 500。</p>
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleNotFound(HttpServletRequest request, Exception e) {
        log.warn("资源不存在 {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Response.fail(ResponseCodeEnum.RESOURCE_NOT_FOUND.getErrorCode(),
                        String.format("接口 %s 不存在", request.getRequestURI())));
    }

    /**
     * 唯一索引冲突。
     *
     * <p>服务层大多会先做存在性检查并给出更友好的业务错误，
     * 这里是并发下的兜底：两个请求同时通过检查时由数据库唯一键拦下。</p>
     */
    @ExceptionHandler({DuplicateKeyException.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleDuplicateKey(HttpServletRequest request, DuplicateKeyException e) {
        log.warn("唯一键冲突 {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Response.fail(ResponseCodeEnum.DUPLICATE_KEY));
    }

    /**
     * 其他未预期异常：堆栈进日志，返回固定文案，不泄漏表名与类名
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ResponseEntity<Response<Object>> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("未预期异常 {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(ResponseCodeEnum.SYSTEM_ERROR));
    }

    // ------------------------------------------------------------------ 内部方法

    private ResponseEntity<Response<Object>> badRequest(HttpServletRequest request, BindingResult bindingResult) {
        // 形如：title 标题不能为空, 当前值: ''; pageSize 每页最多 100 条, 当前值: '999';
        StringBuilder sb = new StringBuilder();
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors -> {
            for (FieldError error : errors) {
                sb.append(error.getField())
                        .append(' ')
                        .append(error.getDefaultMessage())
                        .append(", 当前值: '")
                        .append(error.getRejectedValue())
                        .append("'; ");
            }
        });

        String errorMessage = sb.length() == 0
                ? ResponseCodeEnum.PARAM_NOT_VALID.getErrorMessage()
                : sb.toString();

        log.warn("参数校验失败 {} -> {}", request.getRequestURI(), errorMessage);
        return badRequest(ResponseCodeEnum.PARAM_NOT_VALID, errorMessage);
    }

    private ResponseEntity<Response<Object>> badRequest(ResponseCodeEnum code, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.fail(code.getErrorCode(), message));
    }
}
