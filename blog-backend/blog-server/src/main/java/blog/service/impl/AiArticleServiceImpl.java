package blog.service.impl;

import blog.config.AiSearchProperties;
import blog.dto.ArticleQueryDTO;
import blog.service.AiArticleService;
import blog.service.ArticleService;
import blog.service.ViewCountService;
import blog.vo.ArticleVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 文章服务实现。
 *
 * 设计重点：
 * 1. 不改现有 /api/articles 与 /api/articles/{id} 行为；
 * 2. 优先走 Spring AI + Ollama embedding；
 * 3. 当 AI 或 Redis 不可用时，自动回退到现有关键词搜索/规则推荐。
 */
@Service
@Slf4j
public class AiArticleServiceImpl implements AiArticleService
{
    private static final String SEARCH_CACHE_KEY_PREFIX = "ai:article:search:";
    private static final String SIMILAR_CACHE_KEY_PREFIX = "ai:article:similar:";
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    @Autowired
    private ArticleService articleService;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AiSearchProperties aiSearchProperties;

    @Autowired
    private ViewCountService viewCountService;

    @Override
    public Map<String, Object> searchArticles(String query, int page, int size)
    {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        String normalizedQuery = normalizeQuery(query);
        if (!StringUtils.hasText(normalizedQuery)) {
            return buildPagedResponse(List.of(), safePage, safeSize, "empty");
        }

        String cacheKey = SEARCH_CACHE_KEY_PREFIX + normalizedQuery + ":" + safePage + ":" + safeSize;
        List<ArticleVO> cached = readArticleListCache(cacheKey);
        if (cached != null) {
            return buildPagedResponse(cached, safePage, safeSize, "semantic-cache");
        }

        try {
            if (canUseAi()) {
                List<ArticleVO> semanticArticles = performSemanticSearch(normalizedQuery, safePage, safeSize);
                writeArticleListCache(cacheKey, semanticArticles);
                return buildPagedResponse(semanticArticles, safePage, safeSize, "semantic");
            }
        } catch (Exception e) {
            log.warn("AI 语义搜索失败，回退到关键词搜索：{}", e.getMessage());
        }

        // 回退到现有标题关键词搜索，保证旧行为仍然可用。
        List<ArticleVO> fallbackArticles = fallbackKeywordSearch(normalizedQuery, safePage, safeSize);
        writeArticleListCache(cacheKey, fallbackArticles);
        return buildPagedResponse(fallbackArticles, safePage, safeSize, "fallback-keyword");
    }

    @Override
    public List<ArticleVO> findSimilarArticles(Long articleId, Integer limit)
    {
        if (articleId == null) {
            return List.of();
        }

        int safeLimit = Math.max(limit == null ? aiSearchProperties.getSearch().getDefaultSimilarLimit() : limit, 1);
        String cacheKey = SIMILAR_CACHE_KEY_PREFIX + articleId + ":" + safeLimit;
        List<ArticleVO> cached = readArticleListCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        ArticleVO currentArticle = articleService.findArticleById(articleId);
        if (currentArticle == null || currentArticle.getStatus() == null || currentArticle.getStatus() != 1) {
            return List.of();
        }

        try {
            if (canUseAi()) {
                List<ArticleVO> semanticArticles = performSimilarArticleSearch(currentArticle, safeLimit);
                writeArticleListCache(cacheKey, semanticArticles);
                return semanticArticles;
            }
        } catch (Exception e) {
            log.warn("AI 相关文章推荐失败，回退到规则推荐：{}", e.getMessage());
        }

        List<ArticleVO> fallbackArticles = fallbackSimilarArticles(currentArticle, safeLimit);
        writeArticleListCache(cacheKey, fallbackArticles);
        return fallbackArticles;
    }

    @Override
    public void refreshArticleIndex()
    {
        if (!canUseAi()) {
            return;
        }

        try {
            List<ArticleVO> publishedArticles = loadPublishedArticles();
            for (ArticleVO article : publishedArticles) {
                buildEmbedding(article);
            }
            log.info("AI 文章索引预热完成，文章数量：{}", publishedArticles.size());
        } catch (Exception e) {
            log.warn("AI 文章索引预热失败：{}", e.getMessage());
        }
    }

    private List<ArticleVO> performSemanticSearch(String query, int page, int size)
    {
        float[] queryEmbedding = buildEmbedding(query);
        List<ArticleVO> publishedArticles = loadPublishedArticles();

        List<ScoredArticle> scoredArticles = new ArrayList<>();
        for (ArticleVO article : publishedArticles) {
            float[] articleEmbedding = buildEmbedding(article);
            double similarity = cosineSimilarity(queryEmbedding, articleEmbedding);
            double finalScore = similarity + keywordBoost(article, query) + popularityBoost(article);
            if (finalScore > 0) {
                scoredArticles.add(new ScoredArticle(article, finalScore));
            }
        }

        scoredArticles.sort(Comparator.comparing(ScoredArticle::getScore).reversed());
        return slice(scoredArticles.stream().map(ScoredArticle::getArticle).toList(), page, size);
    }

    private List<ArticleVO> performSimilarArticleSearch(ArticleVO currentArticle, int limit)
    {
        float[] currentEmbedding = buildEmbedding(currentArticle);
        List<ArticleVO> publishedArticles = loadPublishedArticles();
        Set<Long> addedIds = new LinkedHashSet<>();
        List<ScoredArticle> scoredArticles = new ArrayList<>();

        for (ArticleVO article : publishedArticles) {
            if (Objects.equals(article.getId(), currentArticle.getId())) {
                continue;
            }

            float[] articleEmbedding = buildEmbedding(article);
            double similarity = cosineSimilarity(currentEmbedding, articleEmbedding);
            double finalScore = similarity + categoryBoost(currentArticle, article) + tagBoost(currentArticle, article) + popularityBoost(article);
            if (finalScore > 0) {
                scoredArticles.add(new ScoredArticle(article, finalScore));
            }
        }

        scoredArticles.sort(Comparator.comparing(ScoredArticle::getScore).reversed());
        List<ArticleVO> result = new ArrayList<>();
        for (ScoredArticle scoredArticle : scoredArticles) {
            if (result.size() >= limit) {
                break;
            }
            ArticleVO article = scoredArticle.getArticle();
            if (addedIds.add(article.getId())) {
                result.add(article);
            }
        }

        if (result.size() < limit) {
            for (ArticleVO article : fallbackSimilarArticles(currentArticle, limit * 2)) {
                if (result.size() >= limit) {
                    break;
                }
                if (addedIds.add(article.getId())) {
                    result.add(article);
                }
            }
        }
        return result;
    }

    private List<ArticleVO> fallbackKeywordSearch(String query, int page, int size)
    {
        ArticleQueryDTO queryDTO = new ArticleQueryDTO();
        queryDTO.setKeyword(query);
        queryDTO.setStatus(1);
        queryDTO.setSortBy("time");
        List<ArticleVO> articles = safePublishedArticles(articleService.listArticles(queryDTO));
        return slice(articles, page, size);
    }

    private List<ArticleVO> fallbackSimilarArticles(ArticleVO currentArticle, int limit)
    {
        List<ArticleVO> allArticles = loadPublishedArticles();
        List<ArticleVO> sameCategory = allArticles.stream()
                .filter(article -> !Objects.equals(article.getId(), currentArticle.getId()))
                .filter(article -> currentArticle.getCategoryId() != null && Objects.equals(article.getCategoryId(), currentArticle.getCategoryId()))
                .sorted(Comparator.comparing(this::articlePopularityScore).reversed())
                .toList();

        List<ArticleVO> sameTags = allArticles.stream()
                .filter(article -> !Objects.equals(article.getId(), currentArticle.getId()))
                .filter(article -> hasTagOverlap(currentArticle, article))
                .sorted(Comparator.comparing(this::articlePopularityScore).reversed())
                .toList();

        List<ArticleVO> hottest = allArticles.stream()
                .filter(article -> !Objects.equals(article.getId(), currentArticle.getId()))
                .sorted(Comparator.comparing(this::articlePopularityScore).reversed())
                .toList();

        List<ArticleVO> result = new ArrayList<>();
        Set<Long> articleIds = new LinkedHashSet<>();
        appendUntilLimit(result, articleIds, sameCategory, limit);
        appendUntilLimit(result, articleIds, sameTags, limit);
        appendUntilLimit(result, articleIds, hottest, limit);
        return result;
    }

    private void appendUntilLimit(List<ArticleVO> target, Set<Long> articleIds, List<ArticleVO> source, int limit)
    {
        for (ArticleVO article : source) {
            if (target.size() >= limit) {
                return;
            }
            if (articleIds.add(article.getId())) {
                target.add(article);
            }
        }
    }

    private List<ArticleVO> loadPublishedArticles()
    {
        ArticleQueryDTO queryDTO = new ArticleQueryDTO();
        queryDTO.setStatus(1);
        queryDTO.setSortBy("time");
        return safePublishedArticles(articleService.listArticles(queryDTO));
    }

    private List<ArticleVO> safePublishedArticles(List<ArticleVO> articles)
    {
        if (articles == null) {
            return List.of();
        }
        return articles.stream()
                .filter(Objects::nonNull)
                .filter(article -> article.getStatus() == null || article.getStatus() == 1)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> buildPagedResponse(List<ArticleVO> pageData, int page, int size, String source)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", pageData);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("currentPage", page);
        pagination.put("totalPage", pageData.isEmpty() ? 0 : page);
        pagination.put("total", pageData.size());
        pagination.put("size", size);
        result.put("pagination", pagination);

        Map<String, Object> meta = new HashMap<>();
        meta.put("source", source);
        result.put("meta", meta);
        return result;
    }

    private List<ArticleVO> slice(List<ArticleVO> source, int page, int size)
    {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max((page - 1) * size, 0);
        if (fromIndex >= source.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, source.size());
        return new ArrayList<>(source.subList(fromIndex, toIndex));
    }

    private boolean canUseAi()
    {
        return aiSearchProperties.isEnabled() && embeddingModel != null;
    }

    private float[] buildEmbedding(ArticleVO article)
    {
        return buildEmbedding(buildIndexText(article));
    }

    private float[] buildEmbedding(String text)
    {
        return embeddingModel.embed(text);
    }

    private String buildIndexText(ArticleVO article)
    {
        // 只抽取对语义有帮助的字段，避免整篇 markdown 原文直接进入 embedding。
        StringBuilder builder = new StringBuilder();
        appendLine(builder, article.getTitle());
        appendLine(builder, article.getSummary());
        appendLine(builder, article.getCategoryName());
        appendLine(builder, article.getTags());
        appendLine(builder, cleanContent(article.getContent()));
        return builder.toString().trim();
    }

    private void appendLine(StringBuilder builder, String value)
    {
        if (StringUtils.hasText(value)) {
            builder.append(value.trim()).append('\n');
        }
    }

    private String cleanContent(String content)
    {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String cleaned = CODE_BLOCK_PATTERN.matcher(content).replaceAll(" ");
        cleaned = MARKDOWN_IMAGE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MARKDOWN_LINK_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = INLINE_CODE_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace('#', ' ')
                .replace('*', ' ')
                .replace('>', ' ')
                .replace('-', ' ')
                .replace('|', ' ');
        cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();

        int maxLength = aiSearchProperties.getSearch().getMaxContentLength();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private String normalizeQuery(String query)
    {
        return StringUtils.hasText(query) ? MULTI_SPACE_PATTERN.matcher(query.trim()).replaceAll(" ") : "";
    }

    private double cosineSimilarity(float[] left, float[] right)
    {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0D;
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double keywordBoost(ArticleVO article, String query)
    {
        if (!StringUtils.hasText(query)) {
            return 0D;
        }
        String lowerCaseQuery = query.toLowerCase();
        double score = 0D;
        if (containsIgnoreCase(article.getTitle(), lowerCaseQuery)) {
            score += 0.12D;
        }
        if (containsIgnoreCase(article.getSummary(), lowerCaseQuery)) {
            score += 0.06D;
        }
        return score;
    }

    private double categoryBoost(ArticleVO currentArticle, ArticleVO candidateArticle)
    {
        return currentArticle.getCategoryId() != null && Objects.equals(currentArticle.getCategoryId(), candidateArticle.getCategoryId())
                ? 0.15D
                : 0D;
    }

    private double tagBoost(ArticleVO currentArticle, ArticleVO candidateArticle)
    {
        Set<String> currentTags = normalizeTagSet(currentArticle.getTags());
        Set<String> candidateTags = normalizeTagSet(candidateArticle.getTags());
        if (currentTags.isEmpty() || candidateTags.isEmpty()) {
            return 0D;
        }

        long overlap = currentTags.stream().filter(candidateTags::contains).count();
        return overlap * 0.05D;
    }

    private boolean hasTagOverlap(ArticleVO currentArticle, ArticleVO candidateArticle)
    {
        Set<String> currentTags = normalizeTagSet(currentArticle.getTags());
        Set<String> candidateTags = normalizeTagSet(candidateArticle.getTags());
        return currentTags.stream().anyMatch(candidateTags::contains);
    }

    private Set<String> normalizeTagSet(String tags)
    {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptySet();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private double popularityBoost(ArticleVO article)
    {
        return Math.min(articlePopularityScore(article) / 1000D, 0.08D);
    }

    private Integer articlePopularityScore(ArticleVO article)
    {
        int views = article.getViewCount() == null ? 0 : article.getViewCount();
        int redisViews = 0;
        try {
            Integer liveViewCount = viewCountService.getViewCount(article.getId());
            redisViews = liveViewCount == null ? 0 : liveViewCount;
        } catch (Exception ignored) {
            // 浏览量只是排序辅助因子，读取失败不影响主流程。
        }
        int stars = article.getStars() == null ? 0 : article.getStars();
        return Math.max(views, redisViews) + stars * 10;
    }

    private boolean containsIgnoreCase(String source, String lowerCaseQuery)
    {
        return StringUtils.hasText(source) && source.toLowerCase().contains(lowerCaseQuery);
    }

    private List<ArticleVO> readArticleListCache(String cacheKey)
    {
        if (stringRedisTemplate == null) {
            return null;
        }
        try {
            String payload = stringRedisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(payload)) {
                return null;
            }
            List<Long> articleIds = Arrays.stream(payload.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .toList();
            List<ArticleVO> articles = new ArrayList<>();
            for (Long articleId : articleIds) {
                ArticleVO article = articleService.findArticleById(articleId);
                if (article != null && (article.getStatus() == null || article.getStatus() == 1)) {
                    articles.add(article);
                }
            }
            return articles;
        } catch (Exception e) {
            log.warn("读取 AI 缓存失败：{}", e.getMessage());
            return null;
        }
    }

    private void writeArticleListCache(String cacheKey, List<ArticleVO> articles)
    {
        if (stringRedisTemplate == null || articles == null) {
            return;
        }
        try {
            String payload = articles.stream()
                    .map(ArticleVO::getId)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, payload, aiSearchProperties.getSearch().getCacheTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入 AI 缓存失败：{}", e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    private static class ScoredArticle
    {
        private ArticleVO article;
        private double score;
    }
}
