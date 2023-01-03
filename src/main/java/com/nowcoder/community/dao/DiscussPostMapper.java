package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    /*
    * 从offset开始查询最多limit条帖子（userId为可选条件）
    * */
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    /*
    * 查询帖子的个数
    * @Param("userId")指定参数别名，当有且仅有一个参数且该参数是动态sql条件，必须取别名
    * */
    int selectDiscussPostRows(@Param("userId") int userId);

    /* 添加帖子 */
    int insertDiscussPost(DiscussPost discussPost);

    /* 根据帖子id查询帖子详情 */
    DiscussPost selectDiscussPostById(int id);

    /* 更新帖子评论数量 */
    int updateCommentCount(int id, int commentCount);

    /*
    * 修改类型
    * 0：普通
    * 1：置顶
    * */
    int updateType(int id, int type);

    /*
    * 修改状态
    * 0：正常
    * 1：精华
    * 2：删除
    * */
    int updateStatus(int id, int status);

    /* 更新分数 */
    int updateScore(int id, double score);

}
