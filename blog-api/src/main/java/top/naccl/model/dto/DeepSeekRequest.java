package top.naccl.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 请求类，用于接收前端传来的用户问题
public class DeepSeekRequest {
    @JsonProperty("prompt")
    private String prompt;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}

