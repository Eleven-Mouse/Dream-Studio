package blog.task;

import blog.config.AiSearchProperties;
import blog.service.AiArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 文章索引定时预热任务。
 *
 * 首版不引入额外向量库，所以这里通过定时预热 embedding，
 * 尽量减少首次搜索时的冷启动成本。
 */
@Component
@Slf4j
public class ArticleAiIndexRefreshTask
{
    @Autowired
    private AiArticleService aiArticleService;

    @Autowired
    private AiSearchProperties aiSearchProperties;

    @Scheduled(initialDelay = 120000, fixedDelayString = "#{T(java.util.concurrent.TimeUnit).MINUTES.toMillis(@aiSearchProperties.search.refreshMinutes)}")
    public void refreshArticleIndex()
    {
        if (!aiSearchProperties.isEnabled()) {
            return;
        }
        log.info("开始执行 AI 文章索引预热任务");
        aiArticleService.refreshArticleIndex();
    }
}
