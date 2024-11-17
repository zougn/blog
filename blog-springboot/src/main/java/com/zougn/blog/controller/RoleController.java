package com.zougn.blog.controller;

import com.zougn.blog.annotation.OptLog;
import com.zougn.blog.dto.RoleDTO;
import com.zougn.blog.dto.UserRoleDTO;
import com.zougn.blog.service.RoleService;
import com.zougn.blog.vo.ConditionVO;
import com.zougn.blog.vo.PageResult;
import com.zougn.blog.vo.Result;
import com.zougn.blog.vo.RoleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.zougn.blog.constant.OptTypeConst.REMOVE;
import static com.zougn.blog.constant.OptTypeConst.SAVE_OR_UPDATE;

/**
 * 角色控制器
 *
 * @author yezhiqiu
 * @date 2021/07/29
 */
@Api(tags = "角色模块")
@RestController
@AllArgsConstructor
public class RoleController {
    private RoleService roleService;

    /**
     * 查询用户角色选项
     *
     * @return {@link Result<UserRoleDTO>} 用户角色选项
     */
    @ApiOperation(value = "查询用户角色选项")
    @GetMapping("/admin/users/role")
    public Result<List<UserRoleDTO>> listUserRoles() {
        return Result.ok(roleService.listUserRoles());
    }

    /**
     * 查询角色列表
     *
     * @param conditionVO 条件
     * @return {@link Result<RoleDTO>} 角色列表
     */
    @ApiOperation(value = "查询角色列表")
    @GetMapping("/admin/roles")
    public Result<PageResult<RoleDTO>> listRoles(ConditionVO conditionVO) {
        return Result.ok(roleService.listRoles(conditionVO));
    }

    /**
     * 保存或更新角色
     *
     * @param roleVO 角色信息
     * @return {@link Result<>}
     */
    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation(value = "保存或更新角色")
    @PostMapping("/admin/role")
    public Result<?> saveOrUpdateRole(@RequestBody @Valid RoleVO roleVO) {
        roleService.saveOrUpdateRole(roleVO);
        return Result.ok();
    }

    /**
     * 删除角色
     *
     * @param roleIdList 角色id列表
     * @return {@link Result<>}
     */
    @OptLog(optType = REMOVE)
    @ApiOperation(value = "删除角色")
    @DeleteMapping("/admin/roles")
    public Result<?> deleteRoles(@RequestBody List<Integer> roleIdList) {
        roleService.deleteRoles(roleIdList);
        return Result.ok();
    }

}
