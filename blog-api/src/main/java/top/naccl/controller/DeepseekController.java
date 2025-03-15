package top.naccl.controller;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.DeepSeekRequest;
import top.naccl.model.dto.DeepSeekResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@RestController
public class DeepseekController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEEPSEEK_API_KEY = "sk-xxxxx";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @PostMapping("/deepseek")
    public DeepSeekResponse handleDeepSeekRequest(@RequestBody DeepSeekRequest request) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.SECONDS)  // 增加读取超时时间
                .build();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"model\": \"deepseek-chat\", \"messages\": [{\"role\": \"user\", \"content\": \"" + request.getPrompt() + "\"}]}";

        // 使用 okhttp3.RequestBody 创建请求体
        okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, json.getBytes());

        Request apiRequest = new Request.Builder()
                .url(DEEPSEEK_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(apiRequest).execute()) {
            if (!response.isSuccessful()) {
                return new DeepSeekResponse("请求失败：" + response.code());
            }

            return handleNormalResponse(response);

        } catch (IOException e) {
            return new DeepSeekResponse("请求异常：" + e.getMessage());
        }
    }

    private DeepSeekResponse handleNormalResponse(Response response) throws IOException {
        try (ResponseBody body = response.body()) {
            if (body == null) {
                return new DeepSeekResponse("空响应");
            }

            String responseData = body.string();
            JsonNode jsonNode = objectMapper.readTree(responseData);
            if (jsonNode.has("choices") && !jsonNode.get("choices").isEmpty()) {
                JsonNode messageNode = jsonNode.get("choices").get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return new DeepSeekResponse(messageNode.get("content").asText());
                }
            }
            return new DeepSeekResponse("未找到有效回答");
        }
    }


}