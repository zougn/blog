package com.zougn.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zougn.blog.constant.RedisPrefixConst;
import com.zougn.blog.dao.PageDao;
import com.zougn.blog.entity.Page;
import com.zougn.blog.service.PageService;
import com.zougn.blog.service.RedisService;
import com.zougn.blog.util.BeanCopyUtils;
import com.zougn.blog.vo.PageVO;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 页面服务
 *
 * @author yezhiqiu
 * @date 2021/08/07
 */
@Service
@AllArgsConstructor
public class PageServiceImpl extends ServiceImpl<PageDao, Page> implements PageService {
    private RedisService redisService;
    private PageDao pageDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePage(PageVO pageVO) {
        Page page = BeanCopyUtils.copyObject(pageVO, Page.class);
        PageServiceImpl pageService = (PageServiceImpl) AopContext.currentProxy();
        pageService.saveOrUpdate(page);
        // 删除缓存
        redisService.del(RedisPrefixConst.PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePage(Integer pageId) {
        pageDao.deleteById(pageId);
        // 删除缓存
        redisService.del(RedisPrefixConst.PAGE_COVER);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<PageVO> listPages() {
        List<PageVO> pageVOList;
        // 查找缓存信息，不存在则从mysql读取，更新缓存
        Object pageList = redisService.get(RedisPrefixConst.PAGE_COVER);
        if (Objects.nonNull(pageList)) {
            pageVOList = JSON.parseObject(pageList.toString(), List.class);
        } else {
            pageVOList = BeanCopyUtils.copyList(pageDao.selectList(null), PageVO.class);
            redisService.set(RedisPrefixConst.PAGE_COVER, JSON.toJSONString(pageVOList));
        }
        return pageVOList;
    }

}




