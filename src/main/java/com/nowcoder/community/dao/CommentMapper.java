package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> selectCommentByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    /* 增加评论 */
    int insertComment(Comment comment);

    /* 查询某个用户发表过的评论 */
    List<Comment> selectCommentByUserId(int userId, int entityType, int offset, int limit);

    /* 查询某个用户发表过的评论数量 */
    int selectCommentCountByUserId(int userId, int entityType);

    Comment selectCommentById(int id);
}
