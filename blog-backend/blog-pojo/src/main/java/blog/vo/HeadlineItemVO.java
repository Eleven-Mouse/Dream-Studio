package blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新闻头条条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeadlineItemVO implements Serializable {

    private Long id;
    private Long authorId;
    private String authorUsername;
    private String authorNickname;
    private String authorAvatar;
    private String title;
    private String summary;
    private String coverImage;
    private Integer viewCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private Long likeCount;
    private Long commentCount;
    private Boolean liked = false;
}
