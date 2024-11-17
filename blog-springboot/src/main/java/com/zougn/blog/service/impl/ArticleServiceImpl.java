package com.zougn.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zougn.blog.constant.CommonConst;
import com.zougn.blog.constant.RedisPrefixConst;
import com.zougn.blog.dao.ArticleDao;
import com.zougn.blog.dao.ArticleTagDao;
import com.zougn.blog.dao.CategoryDao;
import com.zougn.blog.dao.TagDao;
import com.zougn.blog.dto.*;
import com.zougn.blog.entity.Article;
import com.zougn.blog.entity.ArticleTag;
import com.zougn.blog.entity.Category;
import com.zougn.blog.entity.Tag;
import com.zougn.blog.exception.BizException;
import com.zougn.blog.service.ArticleService;
import com.zougn.blog.service.ArticleTagService;
import com.zougn.blog.service.RedisService;
import com.zougn.blog.service.TagService;
import com.zougn.blog.strategy.context.SearchStrategyContext;
import com.zougn.blog.util.BeanCopyUtils;
import com.zougn.blog.util.PageUtils;
import com.zougn.blog.util.UserUtils;
import com.zougn.blog.vo.*;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.zougn.blog.enums.ArticleStatusEnum.DRAFT;
import static com.zougn.blog.enums.ArticleStatusEnum.PUBLIC;


/**
 * 文章服务
 *
 * @author yezhiqiu
 * @date 2021/08/10
 */
@Service
@AllArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {
    private ArticleDao articleDao;
    private CategoryDao categoryDao;
    private TagDao tagDao;
    private TagService tagService;
    private ArticleTagDao articleTagDao;
    private SearchStrategyContext searchStrategyContext;
    private HttpSession session;
    private RedisService redisService;
    private ArticleTagService articleTagService;

    @Override
    public PageResult<ArchiveDTO> listArchives() {
        Page<Article> page = new Page<>(PageUtils.getCurrent(), PageUtils.getSize());
        // 获取分页数据
        Page<Article> articlePage = articleDao.selectPage(page, new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getCreateTime)
                .orderByDesc(Article::getCreateTime)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus()));
        List<ArchiveDTO> archiveDTOList = BeanCopyUtils.copyList(articlePage.getRecords(), ArchiveDTO.class);
        return new PageResult<>(archiveDTOList, (int) articlePage.getTotal());
    }

    @Override
    public PageResult<ArticleBackDTO> listArticleBacks(ConditionVO condition) {
        // 查询文章总量
        Integer count = articleDao.countArticleBacks(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询后台文章
        List<ArticleBackDTO> articleBackDTOList = articleDao.listArticleBacks(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        // 查询文章点赞量和浏览量
        Map<Object, Double> viewsCountMap = redisService.zAllScore(RedisPrefixConst.ARTICLE_VIEWS_COUNT);
        Map<String, Object> likeCountMap = redisService.hGetAll(RedisPrefixConst.ARTICLE_LIKE_COUNT);
        // 封装点赞量和浏览量
        articleBackDTOList.forEach(item -> {
            Double viewsCount = viewsCountMap.get(item.getId());
            if (Objects.nonNull(viewsCount)) {
                item.setViewsCount(viewsCount.intValue());
            }
            item.setLikeCount((Integer) likeCountMap.get(item.getId().toString()));
        });
        return new PageResult<>(articleBackDTOList, count);
    }

    @Override
    public List<ArticleHomeDTO> listArticles() {
        return articleDao.listArticles(PageUtils.getLimitCurrent(), PageUtils.getSize());
    }

    @Override
    public ArticlePreviewListDTO listArticlesByCondition(ConditionVO condition) {
        // 查询文章
        List<ArticlePreviewDTO> articlePreviewDTOList = articleDao.listArticlesByCondition(PageUtils.getLimitCurrent(), PageUtils.getSize(), condition);
        // 搜索条件对应名(标签或分类名)
        String name;
        if (Objects.nonNull(condition.getCategoryId())) {
            name = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                            .select(Category::getCategoryName)
                            .eq(Category::getId, condition.getCategoryId()))
                    .getCategoryName();
        } else {
            name = tagService.getOne(new LambdaQueryWrapper<Tag>()
                            .select(Tag::getTagName)
                            .eq(Tag::getId, condition.getTagId()))
                    .getTagName();
        }
        return ArticlePreviewListDTO.builder()
                .articlePreviewDTOList(articlePreviewDTOList)
                .name(name)
                .build();
    }

    @Override
    public ArticleDTO getArticleById(Integer articleId) {
        // 查询推荐文章
        CompletableFuture<List<ArticleRecommendDTO>> recommendArticleList = CompletableFuture
                .supplyAsync(() -> articleDao.listRecommendArticles(articleId));
        // 查询最新文章
        CompletableFuture<List<ArticleRecommendDTO>> newestArticleList = CompletableFuture
                .supplyAsync(() -> {
                    List<Article> articleList = articleDao.selectList(new LambdaQueryWrapper<Article>()
                            .select(Article::getId, Article::getArticleTitle, Article::getArticleCover, Article::getCreateTime)
                            .eq(Article::getIsDelete, CommonConst.FALSE)
                            .eq(Article::getStatus, PUBLIC.getStatus())
                            .orderByDesc(Article::getId)
                            .last("limit 5"));
                    return BeanCopyUtils.copyList(articleList, ArticleRecommendDTO.class);
                });
        // 查询id对应文章
        ArticleDTO article = articleDao.getArticleById(articleId);
        if (Objects.isNull(article)) {
            throw new BizException("文章不存在");
        }
        // 更新文章浏览量
        updateArticleViewsCount(articleId);
        // 查询上一篇下一篇文章
        Article lastArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus())
                .lt(Article::getId, articleId)
                .orderByDesc(Article::getId)
                .last("limit 1"));
        Article nextArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, CommonConst.FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus())
                .gt(Article::getId, articleId)
                .orderByAsc(Article::getId)
                .last("limit 1"));
        article.setLastArticle(BeanCopyUtils.copyObject(lastArticle, ArticlePaginationDTO.class));
        article.setNextArticle(BeanCopyUtils.copyObject(nextArticle, ArticlePaginationDTO.class));
        // 封装点赞量和浏览量
        Double score = redisService.zScore(RedisPrefixConst.ARTICLE_VIEWS_COUNT, articleId);
        if (Objects.nonNull(score)) {
            article.setViewsCount(score.intValue());
        }
        article.setLikeCount((Integer) redisService.hGet(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString()));
        // 封装文章信息
        try {
            article.setRecommendArticleList(recommendArticleList.get());
            article.setNewestArticleList(newestArticleList.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return article;
    }

    /**
     * 更新文章浏览量
     *
     * @param articleId 文章id
     */
    public void updateArticleViewsCount(Integer articleId) {
        // 判断是否第一次访问，增加浏览量
        Set<Integer> articleSet = (Set<Integer>) Optional.ofNullable(session.getAttribute(CommonConst.ARTICLE_SET)).orElse(new HashSet<>());
        if (!articleSet.contains(articleId)) {
            articleSet.add(articleId);
            session.setAttribute(CommonConst.ARTICLE_SET, articleSet);
            // 浏览量+1
            redisService.zIncr(RedisPrefixConst.ARTICLE_VIEWS_COUNT, articleId, 1D);
        }
    }


    @Override
    public void saveArticleLike(Integer articleId) {
        // 判断是否点赞
        String articleLikeKey = RedisPrefixConst.ARTICLE_USER_LIKE + UserUtils.getLoginUser().getUserInfoId();
        if (redisService.sIsMember(articleLikeKey, articleId)) {
            // 点过赞则删除文章id
            redisService.sRemove(articleLikeKey, articleId);
            // 文章点赞量-1
            redisService.hDecr(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString(), 1L);
        } else {
            // 未点赞则增加文章id
            redisService.sAdd(articleLikeKey, articleId);
            // 文章点赞量+1
            redisService.hIncr(RedisPrefixConst.ARTICLE_LIKE_COUNT, articleId.toString(), 1L);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateArticle(ArticleVO articleVO) {
        // 保存文章分类
        Category category = saveArticleCategory(articleVO);
        // 保存或修改文章
        Article article = BeanCopyUtils.copyObject(articleVO, Article.class);
        if (Objects.nonNull(category)) {
            article.setCategoryId(category.getId());
        }
        article.setUserId(UserUtils.getLoginUser().getUserInfoId());
        ArticleServiceImpl o = (ArticleServiceImpl) AopContext.currentProxy();
        o.saveOrUpdate(article);
        // 保存文章标签
        saveArticleTag(articleVO, article.getId());
    }

    /**
     * 保存文章分类
     *
     * @param articleVO 文章信息
     * @return {@link Category} 文章分类
     */
    private Category saveArticleCategory(ArticleVO articleVO) {
        // 判断分类是否存在
        Category category = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, articleVO.getCategoryName()));
        if (Objects.isNull(category) && !articleVO.getStatus().equals(DRAFT.getStatus())) {
            category = Category.builder()
                    .categoryName(articleVO.getCategoryName())
                    .build();
            categoryDao.insert(category);
        }
        return category;
    }

    /**
     * 保存文章标签
     *
     * @param articleVO 文章信息
     */
    private void saveArticleTag(ArticleVO articleVO, Integer articleId) {
        // 编辑文章则删除文章所有标签
        if (Objects.nonNull(articleVO.getId())) {
            articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, articleVO.getId()));
        }
        // 添加文章标签
        List<String> tagNameList = articleVO.getTagNameList();
        if (CollectionUtils.isNotEmpty(tagNameList)) {
            // 查询已存在的标签
            List<Tag> existTagList = tagService.list(new LambdaQueryWrapper<Tag>()
                    .in(Tag::getTagName, tagNameList));
            List<String> existTagNameList = existTagList.stream()
                    .map(Tag::getTagName)
                    .collect(Collectors.toList());
            List<Integer> existTagIdList = existTagList.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            // 对比新增不存在的标签
            tagNameList.removeAll(existTagNameList);
            if (CollectionUtils.isNotEmpty(tagNameList)) {
                List<Tag> tagList = tagNameList.stream().map(item -> Tag.builder()
                                .tagName(item)
                                .build())
                        .collect(Collectors.toList());
                tagService.saveBatch(tagList);
                List<Integer> tagIdList = tagList.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                existTagIdList.addAll(tagIdList);
            }
            // 提取标签id绑定文章
            List<ArticleTag> articleTagList = existTagIdList.stream().map(item -> ArticleTag.builder()
                            .articleId(articleId)
                            .tagId(item)
                            .build())
                    .collect(Collectors.toList());
            articleTagService.saveBatch(articleTagList);
        }
    }

    @Override
    public void updateArticleTop(ArticleTopVO articleTopVO) {
        // 修改文章置顶状态
        Article article = Article.builder()
                .id(articleTopVO.getId())
                .isTop(articleTopVO.getIsTop())
                .build();
        articleDao.updateById(article);
    }

    @Override
    public void updateArticleDelete(DeleteVO deleteVO) {
        // 修改文章逻辑删除状态
        List<Article> articleList = deleteVO.getIdList().stream()
                .map(id -> Article.builder()
                        .id(id)
                        .isTop(CommonConst.FALSE)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());
        ArticleServiceImpl o = (ArticleServiceImpl) AopContext.currentProxy();
        o.updateBatchById(articleList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticles(List<Integer> articleIdList) {
        // 删除文章标签关联
        articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIdList));
        // 删除文章
        articleDao.deleteBatchIds(articleIdList);
    }

    @Override
    public List<ArticleSearchDTO> listArticlesBySearch(ConditionVO condition) {
        return searchStrategyContext.executeSearchStrategy(condition.getKeywords());
    }

    @Override
    public ArticleVO getArticleBackById(Integer articleId) {
        // 查询文章信息
        Article article = articleDao.selectById(articleId);
        // 查询文章分类
        Category category = categoryDao.selectById(article.getCategoryId());
        String categoryName = null;
        if (Objects.nonNull(category)) {
            categoryName = category.getCategoryName();
        }
        // 查询文章标签
        List<String> tagNameList = tagDao.listTagNameByArticleId(articleId);
        // 封装数据
        ArticleVO articleVO = BeanCopyUtils.copyObject(article, ArticleVO.class);
        articleVO.setCategoryName(categoryName);
        articleVO.setTagNameList(tagNameList);
        return articleVO;
    }

}
