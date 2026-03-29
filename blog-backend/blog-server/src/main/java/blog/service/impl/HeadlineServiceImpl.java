package blog.service.impl;

import blog.mapper.ArticleLikeMapper;
import blog.mapper.ArticleMapper;
import blog.service.HeadlineService;
import blog.vo.HeadlineItemVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HeadlineServiceImpl implements HeadlineService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Override
    public PageInfo<HeadlineItemVO> pageHeadlines(int page, int size, Long currentUserId) {
        PageHelper.startPage(page, size, "COALESCE(publish_time, create_time) DESC");
        List<HeadlineItemVO> items = articleMapper.selectHeadlineItems();
        if (items == null) {
            items = Collections.emptyList();
        }
        if (currentUserId != null && !items.isEmpty()) {
            List<Long> ids = items.stream().map(HeadlineItemVO::getId).collect(Collectors.toList());
            List<Long> likedIds = articleLikeMapper.selectArticleIdsByUserId(currentUserId, ids);
            Set<Long> likedSet = likedIds == null ? Collections.emptySet() : new HashSet<>(likedIds);
            items.forEach(it -> it.setLiked(likedSet.contains(it.getId())));
        }
        return new PageInfo<>(items);
    }
}
