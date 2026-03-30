package blog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章点赞实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleLike implements Serializable {

    private Long id;
    private Long articleId;
    private Long userId;
    private LocalDateTime createTime;
}
