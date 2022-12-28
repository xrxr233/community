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
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    /*
    * 查询帖子的个数
    * @Param("userId")指定参数别名，当有且仅有一个参数且该参数是动态sql条件，必须取别名
    * */
    int selectDiscussPostRows(@Param("userId") int userId);

    /* 添加帖子 */
    int insertDiscussPost(DiscussPost discussPost);
}
