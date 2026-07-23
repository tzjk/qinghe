package com.qinghe.life.vo;

import com.qinghe.life.entity.Blog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogVO {
    private Long id;
    private Long shopId;
    private String title;
    private String content;
    private String coverImage;
    private Integer likeCount;
    private Integer favoriteCount;

    public static BlogVO fromBlog(Blog blog) {
        return new BlogVO(blog.getId(), blog.getShopId(), blog.getTitle(), blog.getContent(), blog.getCoverImage(),
                blog.getLikeCount(), blog.getFavoriteCount());
    }
}
