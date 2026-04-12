package blog.service.impl;

import blog.entity.ArticleLike;
import blog.mapper.ArticleLikeMapper;
import blog.mapper.ArticleMapper;
import blog.service.ArticleLikeService;
import blog.vo.ArticleVO;
import blog.vo.LikeStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class ArticleLikeServiceImpl implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeStatusVO toggleLike(Long articleId, Long userId, boolean like) {
        ArticleVO article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new IllegalArgumentException("文章不存在或未发布");
        }

        if (like) {
            ArticleLike record = new ArticleLike();
            record.setArticleId(articleId);
            record.setUserId(userId);
            record.setCreateTime(LocalDateTime.now());
            try {
                articleLikeMapper.insert(record);
            } catch (DuplicateKeyException e) {
                log.debug("重复点赞，articleId={}, userId={}", articleId, userId);
            }
        } else {
            articleLikeMapper.deleteByArticleIdAndUserId(articleId, userId);
        }

        Long likeCount = articleLikeMapper.countByArticleId(articleId);
        boolean liked = like && articleLikeMapper.exists(articleId, userId) != null;

        // 同步更新 article 表的 stars 字段
        articleMapper.updateStars(articleId, likeCount);

        return new LikeStatusVO(articleId, likeCount, liked);
    }
}
