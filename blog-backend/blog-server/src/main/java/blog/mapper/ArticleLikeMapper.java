package blog.mapper;

import blog.entity.ArticleLike;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleLikeMapper {

    @Insert("INSERT INTO article_like(article_id, user_id, create_time) VALUES(#{articleId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleLike like);

    @Delete("DELETE FROM article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    int deleteByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM article_like WHERE article_id = #{articleId}")
    Long countByArticleId(Long articleId);

    @Select("SELECT 1 FROM article_like WHERE article_id = #{articleId} AND user_id = #{userId} LIMIT 1")
    Integer exists(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT article_id FROM article_like",
            "WHERE user_id = #{userId}",
            "<if test='articleIds != null and articleIds.size() > 0'>",
            "AND article_id IN",
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    List<Long> selectArticleIdsByUserId(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);
}
