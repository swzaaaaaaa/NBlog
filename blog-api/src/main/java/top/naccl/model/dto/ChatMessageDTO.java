package top.naccl.model.dto;


import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class ChatMessageDTO {
    private Integer id;
    private String content;
    private boolean isUser;
    private boolean isLoading;


    public ChatMessageDTO(Integer id, String content, boolean isUser, boolean isLoading) {
        this.id = id;
        this.content = content;
        this.isUser = isUser;
        this.isLoading = isLoading;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean getIsUser() {
        return isUser;
    }

    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}
