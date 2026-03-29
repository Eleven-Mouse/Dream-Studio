package blog.controller.user;

import blog.dto.LikeToggleRequest;
import blog.entity.UserAccount;
import blog.result.ApiResponse;
import blog.service.ArticleLikeService;
import blog.service.HeadlineService;
import blog.service.UserAccountService;
import blog.vo.HeadlineItemVO;
import blog.vo.LikeStatusVO;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/headlines")
@Slf4j
@ApiOperation("新闻头条专栏")
public class HeadlineController {

    @Autowired
    private HeadlineService headlineService;

    @Autowired
    private ArticleLikeService articleLikeService;

    @Autowired
    private UserAccountService userAccountService;

    @GetMapping
    @ApiOperation("获取新闻头条列表")
    public ApiResponse<Map<String, Object>> listHeadlines(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize,
                                                          Authentication authentication) {
        Long userId = resolveUserId(authentication);
        PageInfo<HeadlineItemVO> pageInfo = headlineService.pageHeadlines(page, pageSize, userId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", pageInfo.getPageNum());
        payload.put("pageSize", pageInfo.getPageSize());
        payload.put("total", pageInfo.getTotal());
        payload.put("list", pageInfo.getList());
        return ApiResponse.success(payload);
    }

    @PostMapping("/{id}/like")
    @ApiOperation("点赞/取消点赞文章")
    public ApiResponse<LikeStatusVO> toggleLike(@PathVariable Long id,
                                                @RequestBody(required = false) LikeToggleRequest request,
                                                Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return ApiResponse.failure(401, "请先登录");
        }
        UserAccount account = userAccountService.findByUsername(authentication.getName());
        if (account == null) {
            return ApiResponse.failure(401, "未找到当前登录用户");
        }
        boolean like = request == null || request.getLike() == null || Boolean.TRUE.equals(request.getLike());
        try {
            LikeStatusVO vo = articleLikeService.toggleLike(id, account.getId(), like);
            return ApiResponse.success("success", vo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(404, e.getMessage());
        } catch (Exception e) {
            log.error("点赞失败", e);
            return ApiResponse.failure(500, "点赞失败，请稍后再试");
        }
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) return null;
        UserAccount account = userAccountService.findByUsername(authentication.getName());
        return account != null ? account.getId() : null;
    }
}
