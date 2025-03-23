package top.naccl.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.naccl.entity.ChatMessage;

import java.util.List;

@Mapper
@Repository
public interface ChatMessageMapper {
    @Insert("INSERT INTO chat_history (content, is_user) VALUES (#{content}, #{isUser})")
    void insertChatMessage(ChatMessage chatMessage);

    @Select("SELECT id, content, is_user as isUser, false as isLoading, created_at as createdAt FROM chat_history ORDER BY created_at ASC")
    List<ChatMessage> getAllChatMessages();
}
