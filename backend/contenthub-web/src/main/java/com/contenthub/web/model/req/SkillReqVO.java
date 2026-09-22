package com.contenthub.web.model.req;

import com.contenthub.web.model.vo.SkillStepVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 编辑 Skill 的请求体（仅管理员）。
 *
 * <p>刻意没有 {@code status} 字段：新建一律落库为 DRAFT，上架与下架只能走
 * {@code /admin/skills/{id}/publish} 与 {@code /offline}。
 * 允许在新增/编辑里直接传状态，会让「先草稿后上架」这个约束形同虚设。</p>
 */
@Data
public class SkillReqVO implements Serializable {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过 100 个字符")
    private String name;

    @Size(max = 16, message = "图标过长")
    private String icon;

    private Long categoryId;

    @Size(max = 500, message = "简介长度不能超过 500 个字符")
    private String summary;

    @Size(max = 1000, message = "「为什么收录」长度不能超过 1000 个字符")
    private String whyIncluded;

    @Size(max = 100, message = "作者长度不能超过 100 个字符")
    private String author;

    @Size(max = 200, message = "仓库地址过长")
    private String repo;

    @Size(max = 500, message = "官网地址过长")
    private String officialUrl;

    @Size(max = 500, message = "安装命令过长")
    private String installCommand;

    @Min(value = 0, message = "星数不能为负")
    private Integer stars;

    @Size(max = 50, message = "版本号过长")
    private String version;

    @Size(max = 50, message = "许可证过长")
    private String license;

    @Size(max = 50, message = "体积描述过长")
    private String size;

    @Min(value = 0, message = "下载量不能为负")
    private Integer downloads;

    @Min(value = 0, message = "安全评级最低 0 级")
    @Max(value = 5, message = "安全评级最高 5 级")
    private Integer securityLevel;

    @Size(max = 50, message = "安全评级文案过长")
    private String securityLabel;

    @Size(max = 100, message = "提交人长度不能超过 100 个字符")
    private String submitter;

    private LocalDate submitTime;

    private List<String> platforms;
    private List<String> tags;
    private List<String> features;
    private List<SkillStepVO> quickStart;

    private Integer teamMaintainers;
    private Integer teamContributors;
    private Integer teamOpenIssues;
    private LocalDate teamLastCommit;

    /** FREE / MEMBER；为空时新增默认 FREE */
    private String accessType;
}
