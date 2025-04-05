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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.naccl.entity.ChatMessage;
import top.naccl.mapper.ChatMessageMapper;
import top.naccl.model.dto.ChatMessageDTO;


@RestController
public class DeepseekController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEEPSEEK_API_KEY = "sk-xxxxx";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Autowired
    private ChatMessageMapper chatMessageMapper;


    @GetMapping(value = "/deepseek", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> handleDeepSeekRequest(@RequestParam("prompt") String prompt) {
        // 保存用户消息到数据库
        ChatMessage userMessage = new ChatMessage();
        userMessage.setContent(prompt);
        userMessage.setUser(true);
        userMessage.setCreatedAt(new Date());
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
                                        assistantMessage.setCreatedAt(new Date());
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

    /**
     * 根据日期查询聊天记录
     * @param date 日期
     * @return 聊天记录列表
     */
    @GetMapping("/chat-history")
    public List<ChatMessageDTO> getChatHistory(@RequestParam(required = false) String date) {
        // 处理日期参数，如果为空则使用当前日期
        Date targetDate = getTargetDate(date);
        // 获取对应日期的聊天记录
        List<ChatMessage> chatMessages = chatMessageMapper.getChatMessagesByDate(targetDate);
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

    /**
     * 查询历史聊天日期
     */
    @GetMapping("/chat-history-dates")
    public List<Date> getDistinctChatDates() {
        List<Date> date = chatMessageMapper.findDistinctCreatedDates();
        for (Date d : date) {
            System.out.println(d);
        }
        return chatMessageMapper.findDistinctCreatedDates();
    }

    private Date getTargetDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            if (date != null && !date.isEmpty()) {
                return sdf.parse(date);
            } else {
                return new Date();
            }
        } catch (ParseException e) {
            // 处理日期格式错误，这里简单返回当前日期
            return new Date();
        }
    }

}