package top.naccl.model.dto;

// 响应类，用于返回 DeepSeek 的回答给前端
public class DeepSeekResponse {
    private String answer;

    public DeepSeekResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
