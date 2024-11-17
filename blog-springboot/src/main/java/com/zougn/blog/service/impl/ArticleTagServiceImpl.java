package com.zougn.blog.service.impl;

import com.zougn.blog.entity.ArticleTag;
import com.zougn.blog.dao.ArticleTagDao;
import com.zougn.blog.service.ArticleTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 文章标签服务
 *
 * @author yezhiqiu
 * @date 2021/08/10
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagDao, ArticleTag> implements ArticleTagService {

}
