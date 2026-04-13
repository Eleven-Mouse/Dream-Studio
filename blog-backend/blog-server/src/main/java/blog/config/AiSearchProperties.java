package blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 语义搜索配置。
 *
 * 这里单独抽出配置，是为了把 AI 能力和现有文章主链路解耦：
 * 即使 Ollama 暂时不可用，系统也可以通过回退逻辑继续工作。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.ai")
public class AiSearchProperties
{
    private boolean enabled = true;

    private final Search search = new Search();

    @Data
    public static class Search
    {
        /**
         * Redis 缓存秒数。
         */
        private long cacheTtlSeconds = 900;

        /**
         * 参与 embedding 的正文最大长度，避免整篇 markdown 直接送入模型造成噪音和开销。
         */
        private int maxContentLength = 2400;

        /**
         * 相关文章默认数量。
         */
        private int defaultSimilarLimit = 5;

        /**
         * 定时刷新缓存的分钟数。
         */
        private long refreshMinutes = 30;
    }
}
