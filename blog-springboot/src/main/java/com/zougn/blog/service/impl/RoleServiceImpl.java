package com.zougn.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zougn.blog.constant.CommonConst;
import com.zougn.blog.dao.RoleDao;
import com.zougn.blog.dao.UserRoleDao;
import com.zougn.blog.dto.RoleDTO;
import com.zougn.blog.dto.UserRoleDTO;
import com.zougn.blog.entity.Role;
import com.zougn.blog.entity.RoleMenu;
import com.zougn.blog.entity.RoleResource;
import com.zougn.blog.entity.UserRole;
import com.zougn.blog.exception.BizException;
import com.zougn.blog.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.zougn.blog.service.RoleMenuService;
import com.zougn.blog.service.RoleResourceService;
import com.zougn.blog.service.RoleService;
import com.zougn.blog.util.BeanCopyUtils;
import com.zougn.blog.util.PageUtils;
import com.zougn.blog.vo.ConditionVO;
import com.zougn.blog.vo.PageResult;
import com.zougn.blog.vo.RoleVO;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
@AllArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements RoleService {
    private RoleDao roleDao;
    private RoleResourceService roleResourceService;
    private RoleMenuService roleMenuService;
    private UserRoleDao userRoleDao;
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;

    @Override
    public List<UserRoleDTO> listUserRoles() {
        // 查询角色列表
        List<Role> roleList = roleDao.selectList(new LambdaQueryWrapper<Role>()
                .select(Role::getId, Role::getRoleName));
        return BeanCopyUtils.copyList(roleList, UserRoleDTO.class);
    }

    @Override
    public PageResult<RoleDTO> listRoles(ConditionVO conditionVO) {
        // 查询角色列表
        List<RoleDTO> roleDTOList = roleDao.listRoles(PageUtils.getLimitCurrent(), PageUtils.getSize(), conditionVO);
        // 查询总量
        Integer count = roleDao.selectCount(new LambdaQueryWrapper<Role>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Role::getRoleName, conditionVO.getKeywords()));
        return new PageResult<>(roleDTOList, count);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateRole(RoleVO roleVO) {
        // 判断角色名重复
        Role existRole = roleDao.selectOne(new LambdaQueryWrapper<Role>()
                .select(Role::getId)
                .eq(Role::getRoleName, roleVO.getRoleName()));
        if (Objects.nonNull(existRole) && !existRole.getId().equals(roleVO.getId())) {
            throw new BizException("角色名已存在");
        }
        // 保存或更新角色信息
        Role role = Role.builder()
                .id(roleVO.getId())
                .roleName(roleVO.getRoleName())
                .roleLabel(roleVO.getRoleLabel())
                .isDisable(CommonConst.FALSE)
                .build();
        RoleServiceImpl roleService = (RoleServiceImpl) AopContext.currentProxy();
        roleService.saveOrUpdate(role);
        // 更新角色资源关系
        if (CollectionUtils.isNotEmpty(roleVO.getResourceIdList())) {
            if (Objects.nonNull(roleVO.getId())) {
                roleResourceService.remove(new LambdaQueryWrapper<RoleResource>()
                        .eq(RoleResource::getRoleId, roleVO.getId()));
            }
            List<RoleResource> roleResourceList = roleVO.getResourceIdList().stream()
                    .map(resourceId -> RoleResource.builder()
                            .roleId(role.getId())
                            .resourceId(resourceId)
                            .build())
                    .collect(Collectors.toList());
            roleResourceService.saveBatch(roleResourceList);
            // 重新加载角色资源信息
            filterInvocationSecurityMetadataSource.clearDataSource();
        }
        // 更新角色菜单关系
        if (CollectionUtils.isNotEmpty(roleVO.getMenuIdList())) {
            if (Objects.nonNull(roleVO.getId())) {
                roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleVO.getId()));
            }
            List<RoleMenu> roleMenuList = roleVO.getMenuIdList().stream()
                    .map(menuId -> RoleMenu.builder()
                            .roleId(role.getId())
                            .menuId(menuId)
                            .build())
                    .collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenuList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteRoles(List<Integer> roleIdList) {
        // 判断角色下是否有用户
        Integer count = userRoleDao.selectCount(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getRoleId, roleIdList));
        if (count > 0) {
            throw new BizException("该角色下存在用户");
        }
        roleDao.deleteBatchIds(roleIdList);
    }

}
