package blog.service;

import blog.vo.HeadlineItemVO;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

@Service
public interface HeadlineService {

    PageInfo<HeadlineItemVO> pageHeadlines(int page, int size, Long currentUserId);
}
