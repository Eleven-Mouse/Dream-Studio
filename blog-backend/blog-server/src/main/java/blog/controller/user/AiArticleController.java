package blog.controller.user;

import blog.result.Result;
import blog.service.AiArticleService;
import blog.vo.ArticleVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 文章接口。
 *
 * 单独新增控制器而不是直接改 ArticleController，
 * 是为了把 AI 增强能力与现有文章主接口隔离，尽量降低回归风险。
 */
@RestController
@RequestMapping("/api/ai/articles")
@Slf4j
public class AiArticleController
{
    @Autowired
    private AiArticleService aiArticleService;

    @GetMapping("/search")
    @ApiOperation("AI 语义搜索文章")
    public Result<Map<String, Object>> searchArticles(@RequestParam("q") String query,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size)
    {
        if (!StringUtils.hasText(query)) {
            return Result.error("搜索内容不能为空");
        }

        return Result.success(aiArticleService.searchArticles(query, page, size));
    }

    @GetMapping("/{articleId}/similar")
    @ApiOperation("获取相关文章")
    public Result<List<ArticleVO>> similarArticles(@PathVariable Long articleId,
                                                   @RequestParam(required = false) Integer limit)
    {
        log.info("获取相关文章，articleId：{}，limit：{}", articleId, limit);
        return Result.success(aiArticleService.findSimilarArticles(articleId, limit));
    }
}
