package blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeStatusVO {
    private Long articleId;
    private Long likeCount;
    private Boolean liked;
}
