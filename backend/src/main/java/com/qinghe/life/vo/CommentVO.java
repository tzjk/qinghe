package com.qinghe.life.vo;

import com.qinghe.life.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;
    private Long userId;
    private String content;
    private Integer score;
    private String images;
    private LocalDateTime createTime;

    public static CommentVO fromComment(Comment comment) {
        return new CommentVO(comment.getId(), comment.getUserId(), comment.getContent(), comment.getScore(),
                comment.getImages(), comment.getCreateTime());
    }
}
