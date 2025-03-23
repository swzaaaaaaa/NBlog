package top.naccl.entity;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ChatMessage {
    private Integer id;
    private String content;
    private boolean isUser;
    private boolean isLoading = false;
    private Date createdAt;
    private String role = "user";
}
