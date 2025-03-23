package top.naccl.controller;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.naccl.entity.ChatMessage;
import top.naccl.mapper.ChatMessageMapper;
import top.naccl.model.dto.ChatMessageDTO;

import javax.servlet.http.HttpSession;


@RestController
public class DeepseekController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEEPSEEK_API_KEY = "sk-xxxxx";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Autowired
    private ChatMessageMapper chatMessageMapper;
//    @PostMapping("/deepseek")
//    public DeepSeekResponse handleDeepSeekRequest(@RequestBody DeepSeekRequest request) throws IOException {
//        OkHttpClient client = new OkHttpClient.Builder()
//                .readTimeout(100, TimeUnit.SECONDS)  // 增加读取超时时间
//                .build();
//
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"model\": \"deepseek-chat\", \"messages\": [{\"role\": \"user\", \"content\": \"" + request.getPrompt() + "\"}]}";
//
//        // 使用 okhttp3.RequestBody 创建请求体
//        okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, json.getBytes());
//
//        Request apiRequest = new Request.Builder()
//                .url(DEEPSEEK_API_URL)
//                .post(body)
//                .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
//                .addHeader("Content-Type", "application/json")
//                .build();
//
//        try (Response response = client.newCall(apiRequest).execute()) {
//            if (!response.isSuccessful()) {
//                return new DeepSeekResponse("请求失败：" + response.code());
//            }
//
//            return handleNormalResponse(response);
//
//        } catch (IOException e) {
//            return new DeepSeekResponse("请求异常：" + e.getMessage());
//        }
//    }
//
//    private DeepSeekResponse handleNormalResponse(Response response) throws IOException {
//        try (ResponseBody body = response.body()) {
//            if (body == null) {
//                return new DeepSeekResponse("空响应");
//            }
//
//            String responseData = body.string();
//            JsonNode jsonNode = objectMapper.readTree(responseData);
//            if (jsonNode.has("choices") && !jsonNode.get("choices").isEmpty()) {
//                JsonNode messageNode = jsonNode.get("choices").get(0).get("message");
//                if (messageNode != null && messageNode.has("content")) {
//                    return new DeepSeekResponse(messageNode.get("content").asText());
//                }
//            }
//            return new DeepSeekResponse("未找到有效回答");
//        }
//    }


    @GetMapping(value = "/deepseek", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleDeepSeekRequest(@RequestParam("prompt") String prompt) {
        // 保存用户消息到数据库
        ChatMessage userMessage = new ChatMessage();
        userMessage.setContent(prompt);
        userMessage.setUser(true);
        chatMessageMapper.insertChatMessage(userMessage);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");

        String json = "{\"model\": \"deepseek-chat\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}], \"stream\": true}";

        okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, json.getBytes());

        Request apiRequest = new Request.Builder()
                .url(DEEPSEEK_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        return Flux.create(emitter -> {
            try {
                client.newCall(apiRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.err.println("请求 DeepSeek API 失败: " + e.getMessage());
                        emitter.error(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody != null) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                                String line;
                                StringBuilder assistantReply = new StringBuilder();
                                while ((line = reader.readLine()) != null) {
                                    if (!line.startsWith("data: ")) {
                                        //System.err.println("跳过无效行: " + line);
                                        continue;
                                    }
                                    String data = line.substring(6).trim();
                                    if (data.equals("[DONE]")) {
                                        // 确保正确发送 [DONE] 信号
                                        emitter.next(ServerSentEvent.builder("[DONE]").build());
                                        emitter.complete();
                                        // 保存助手的回复到数据库
                                        ChatMessage assistantMessage = new ChatMessage();
                                        assistantMessage.setContent(assistantReply.toString());
                                        assistantMessage.setUser(false);
                                        chatMessageMapper.insertChatMessage(assistantMessage);
                                        break;
                                    }
                                    try {
                                        JsonNode jsonNode = objectMapper.readTree(data);
                                        if (jsonNode.has("choices") && !jsonNode.get("choices").isEmpty()) {
                                            JsonNode deltaNode = jsonNode.get("choices").get(0).get("delta");
                                            if (deltaNode != null && deltaNode.has("content")) {
                                                String content = deltaNode.get("content").asText();
                                                assistantReply.append(content);
                                                emitter.next(ServerSentEvent.builder(content).build());
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("解析错误: " + e.getMessage());
                                        emitter.error(e);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("处理请求时出现异常: " + e.getMessage());
                emitter.error(e);
            }
        });
    }


    @GetMapping("/chat-history")
    public List<ChatMessageDTO> getChatHistory() {
        List<ChatMessage> chatMessages = chatMessageMapper.getAllChatMessages();
        List<ChatMessageDTO> chatMessageDTOS = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            chatMessageDTOS.add(new ChatMessageDTO(
                    chatMessage.getId(),
                    chatMessage.getContent(),
                    chatMessage.isUser(),
                    chatMessage.isLoading()
            ));
        }
        return chatMessageDTOS;
    }
}