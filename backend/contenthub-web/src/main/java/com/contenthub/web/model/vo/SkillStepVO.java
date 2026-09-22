package com.contenthub.web.model.vo;

import java.io.Serializable;

/**
 * 快速上手的一步。
 *
 * <p>用 record 是因为它既要做接口出参，又要作为 {@code skill.quick_start}
 * 这一列的反序列化目标（见 {@code JsonUtil.parseList}），字段不可变最省心。</p>
 */
public record SkillStepVO(String title, String detail) implements Serializable {
}
