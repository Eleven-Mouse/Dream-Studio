package blog.service;

import blog.vo.LikeStatusVO;
import org.springframework.stereotype.Service;

@Service
public interface ArticleLikeService {

    LikeStatusVO toggleLike(Long articleId, Long userId, boolean like);
}
