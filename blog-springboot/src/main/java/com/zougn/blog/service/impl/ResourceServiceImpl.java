package com.zougn.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zougn.blog.constant.CommonConst;
import com.zougn.blog.dao.ResourceDao;
import com.zougn.blog.dao.RoleResourceDao;
import com.zougn.blog.dto.LabelOptionDTO;
import com.zougn.blog.dto.ResourceDTO;
import com.zougn.blog.entity.Resource;
import com.zougn.blog.entity.RoleResource;
import com.zougn.blog.exception.BizException;
import com.zougn.blog.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.zougn.blog.service.ResourceService;
import com.zougn.blog.util.BeanCopyUtils;
import com.zougn.blog.vo.ConditionVO;
import com.zougn.blog.vo.ResourceVO;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源服务
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
@AllArgsConstructor
public class ResourceServiceImpl extends ServiceImpl<ResourceDao, Resource> implements ResourceService {
    private RestTemplate restTemplate;
    private ResourceDao resourceDao;
    private RoleResourceDao roleResourceDao;
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSwagger() {
        // 删除所有资源
        this.remove(null);
        roleResourceDao.delete(null);
        List<Resource> resourceList = new ArrayList<>();
        Map<String, Object> data = restTemplate.getForObject("http://localhost:8080/v2/api-docs", Map.class);
        // 获取所有模块
        List<Map<String, String>> tagList =  Optional.ofNullable(data).map(e->(List<Map<String, String>>)e.get("tags")).orElse(new ArrayList<>());
        tagList.forEach(item -> {
            Resource resource = Resource.builder()
                    .resourceName(item.get("name"))
                    .isAnonymous(CommonConst.FALSE)
                    .build();
            resourceList.add(resource);
        });
        ResourceServiceImpl resourceService = (ResourceServiceImpl) AopContext.currentProxy();
        resourceService.saveBatch(resourceList);
        Map<String, Integer> permissionMap = resourceList.stream()
                .collect(Collectors.toMap(Resource::getResourceName, Resource::getId));
        resourceList.clear();
        // 获取所有接口
        Map<String, Map<String, Map<String, Object>>> path = (Map<String, Map<String, Map<String, Object>>>) data.get("paths");
        path.forEach((url, value) -> value.forEach((requestMethod, info) -> {
            String permissionName = info.get("summary").toString();
            List<String> tag = (List<String>) info.get("tags");
            Integer parentId = permissionMap.get(tag.get(0));
            Resource resource = Resource.builder()
                    .resourceName(permissionName)
                    .url(url.replaceAll("\\{[^}]*\\}", "*"))
                    .parentId(parentId)
                    .requestMethod(requestMethod.toUpperCase())
                    .isAnonymous(CommonConst.FALSE)
                    .build();
            resourceList.add(resource);
        }));

        resourceService.saveBatch(resourceList);
    }

    @Override
    public void saveOrUpdateResource(ResourceVO resourceVO) {
        // 更新资源信息
        Resource resource = BeanCopyUtils.copyObject(resourceVO, Resource.class);
        ResourceServiceImpl resourceService = (ResourceServiceImpl) AopContext.currentProxy();

        resourceService.saveOrUpdate(resource);
        // 重新加载角色资源信息
        filterInvocationSecurityMetadataSource.clearDataSource();
    }

    @Override
    public void deleteResource(Integer resourceId) {
        // 查询是否有角色关联
        Integer count = roleResourceDao.selectCount(new LambdaQueryWrapper<RoleResource>()
                .eq(RoleResource::getResourceId, resourceId));
        if (count > 0) {
            throw new BizException("该资源下存在角色");
        }
        // 删除子资源
        List<Integer> resourceIdList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                        .select(Resource::getId).
                        eq(Resource::getParentId, resourceId))
                .stream()
                .map(Resource::getId)
                .collect(Collectors.toList());
        resourceIdList.add(resourceId);
        resourceDao.deleteBatchIds(resourceIdList);
    }

    @Override
    public List<ResourceDTO> listResources(ConditionVO conditionVO) {
        // 查询资源列表
        List<Resource> resourceList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Resource::getResourceName, conditionVO.getKeywords()));
        // 获取所有模块
        List<Resource> parentList = listResourceModule(resourceList);
        // 根据父id分组获取模块下的资源
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resourceList);
        // 绑定模块下的所有接口
        List<ResourceDTO> resourceDTOList = parentList.stream().map(item -> {
            ResourceDTO resourceDTO = BeanCopyUtils.copyObject(item, ResourceDTO.class);
            List<ResourceDTO> childrenList = BeanCopyUtils.copyList(childrenMap.get(item.getId()), ResourceDTO.class);
            resourceDTO.setChildren(childrenList);
            childrenMap.remove(item.getId());
            return resourceDTO;
        }).collect(Collectors.toList());
        // 若还有资源未取出则拼接
        if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Resource> childrenList = new ArrayList<>();
            childrenMap.values().forEach(childrenList::addAll);
            List<ResourceDTO> childrenDTOList = childrenList.stream()
                    .map(item -> BeanCopyUtils.copyObject(item, ResourceDTO.class))
                    .collect(Collectors.toList());
            resourceDTOList.addAll(childrenDTOList);
        }
        return resourceDTOList;
    }

    @Override
    public List<LabelOptionDTO> listResourceOption() {
        // 查询资源列表
        List<Resource> resourceList = resourceDao.selectList(new LambdaQueryWrapper<Resource>()
                .select(Resource::getId, Resource::getResourceName, Resource::getParentId)
                .eq(Resource::getIsAnonymous, CommonConst.FALSE));
        // 获取所有模块
        List<Resource> parentList = listResourceModule(resourceList);
        // 根据父id分组获取模块下的资源
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resourceList);
        // 组装父子数据
        return parentList.stream().map(item -> {
            List<LabelOptionDTO> list = new ArrayList<>();
            List<Resource> children = childrenMap.get(item.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                list = children.stream()
                        .map(resource -> LabelOptionDTO.builder()
                                .id(resource.getId())
                                .label(resource.getResourceName())
                                .build())
                        .collect(Collectors.toList());
            }
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getResourceName())
                    .children(list)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取模块下的所有资源
     *
     * @param resourceList 资源列表
     * @return 模块资源
     */
    private Map<Integer, List<Resource>> listResourceChildren(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.nonNull(item.getParentId()))
                .collect(Collectors.groupingBy(Resource::getParentId));
    }

    /**
     * 获取所有资源模块
     *
     * @param resourceList 资源列表
     * @return 资源模块列表
     */
    private List<Resource> listResourceModule(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.isNull(item.getParentId()))
                .collect(Collectors.toList());
    }

}
