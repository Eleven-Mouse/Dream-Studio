package blog.service;

import blog.vo.ArticleVO;

import java.util.List;
import java.util.Map;

/**
 * AI 文章服务。
 *
 * 这里不直接改造 ArticleService，而是新增一个独立服务，
 * 这样可以在不影响原有文章列表/详情逻辑的前提下增加 AI 能力。
 */
public interface AiArticleService
{
    Map<String, Object> searchArticles(String query, int page, int size);

    List<ArticleVO> findSimilarArticles(Long articleId, Integer limit);

    void refreshArticleIndex();
}
