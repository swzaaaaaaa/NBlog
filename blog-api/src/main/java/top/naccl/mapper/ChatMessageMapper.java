package top.naccl.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.naccl.entity.ChatMessage;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface ChatMessageMapper {
    @Insert("INSERT INTO chat_history (content, is_user,created_at) VALUES (#{content}, #{isUser},#{createdAt})")
    void insertChatMessage(ChatMessage chatMessage);

    @Select("SELECT id, content, is_user as isUser, false as isLoading, created_at as createdAt FROM chat_history ORDER BY created_at ASC")
    List<ChatMessage> getAllChatMessages();

    /**
     * 根据日期查询聊天记录
     * @param date 日期
     * @return 聊天记录列表
     */
    @Select("SELECT id, content, is_user as isUser, false as isLoading, created_at as createdAt FROM chat_history WHERE DATE(created_at) = #{date} ORDER BY created_at ASC")
    List<ChatMessage> getChatMessagesByDate(Date date);


    /**
     * 查询历史聊天日期
     */
    @Select("SELECT DISTINCT DATE(created_at) FROM chat_history")
    List<Date> findDistinctCreatedDates();

}
