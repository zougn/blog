package com.zougn.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zougn.blog.dao.FriendLinkDao;
import com.zougn.blog.dto.FriendLinkBackDTO;
import com.zougn.blog.dto.FriendLinkDTO;
import com.zougn.blog.entity.FriendLink;
import com.zougn.blog.service.FriendLinkService;
import com.zougn.blog.util.BeanCopyUtils;
import com.zougn.blog.util.PageUtils;
import com.zougn.blog.vo.ConditionVO;
import com.zougn.blog.vo.FriendLinkVO;
import com.zougn.blog.vo.PageResult;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 友情链接服务
 *
 * @author xiaojie
 * @date 2021/08/10
 */
@Service
@AllArgsConstructor
public class FriendLinkServiceImpl extends ServiceImpl<FriendLinkDao, FriendLink> implements FriendLinkService {
    private FriendLinkDao friendLinkDao;

    @Override
    public List<FriendLinkDTO> listFriendLinks() {
        // 查询友链列表
        List<FriendLink> friendLinkList = friendLinkDao.selectList(null);
        return BeanCopyUtils.copyList(friendLinkList, FriendLinkDTO.class);
    }

    @Override
    public PageResult<FriendLinkBackDTO> listFriendLinkDTO(ConditionVO condition) {
        // 分页查询友链列表
        Page<FriendLink> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        Page<FriendLink> friendLinkPage = friendLinkDao.selectPage(page, new LambdaQueryWrapper<FriendLink>()
                .like(StringUtils.isNotBlank(condition.getKeywords()), FriendLink::getLinkName, condition.getKeywords()));
        // 转换DTO
        List<FriendLinkBackDTO> friendLinkBackDTOList = BeanCopyUtils.copyList(friendLinkPage.getRecords(), FriendLinkBackDTO.class);
        return new PageResult<>(friendLinkBackDTOList, (int) friendLinkPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO) {
        FriendLinkServiceImpl friendLinkService = (FriendLinkServiceImpl) AopContext.currentProxy();
        FriendLink friendLink = BeanCopyUtils.copyObject(friendLinkVO, FriendLink.class);
        friendLinkService.saveOrUpdate(friendLink);
    }

}
