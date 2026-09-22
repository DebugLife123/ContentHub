package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillPageReqVO;
import com.contenthub.web.model.vo.SkillDetailVO;
import com.contenthub.web.model.vo.SkillListVO;
import com.contenthub.web.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Skill 商城（公开读）。
 *
 * <p>只暴露已上架的 Skill。会员专属的细节判定在 Service 里，
 * 未解锁时不会把安装命令与上手步骤发给前端。</p>
 */
@RestController
@RequestMapping("/skills")
@Tag(name = "Skill 商城")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "Skill 分页查询（仅已上架，公开；默认按星数倒序）")
    public Response<PageResponse<SkillListVO>> page(@Validated SkillPageReqVO req) {
        return skillService.pagePublished(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Skill 详情（仅已上架；无会员权限时不下发安装方式）")
    public Response<SkillDetailVO> detail(@PathVariable Long id) {
        return skillService.findPublishedById(id);
    }
}
