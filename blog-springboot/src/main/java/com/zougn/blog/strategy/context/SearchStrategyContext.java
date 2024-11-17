package com.zougn.blog.strategy.context;

import com.zougn.blog.dto.ArticleSearchDTO;
import com.zougn.blog.strategy.SearchStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.zougn.blog.enums.SearchModeEnum.getStrategy;

/**
 * 搜索策略上下文
 *
 * @author yezhiqiu
 * @date 2021/07/27
 */
@Service
public class SearchStrategyContext {
    /**
     * 搜索模式
     */

    private String searchMode;

    private Map<String, SearchStrategy> searchStrategyMap;

    public SearchStrategyContext(@Value("${search.mode}")String searchMode, Map<String, SearchStrategy> searchStrategyMap) {
        this.searchMode = searchMode;
        this.searchStrategyMap = searchStrategyMap;
    }

    /**
     * 执行搜索策略
     *
     * @param keywords 关键字
     * @return {@link List<ArticleSearchDTO>} 搜索文章
     */
    public List<ArticleSearchDTO> executeSearchStrategy(String keywords) {
        return searchStrategyMap.get(getStrategy(searchMode)).searchArticle(keywords);
    }

}
