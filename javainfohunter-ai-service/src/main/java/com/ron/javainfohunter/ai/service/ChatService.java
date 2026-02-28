package com.ron.javainfohunter.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * AI 聊天服务
 * <p>
 * 提供简单的聊天接口，封装 ChatClient
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    /**
     * 发送聊天消息
     *
     * @param message 消息内容
     * @return AI 响应
     */
    public String chat(String message) {
        log.debug("Chat input: {}", message);
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();
        log.debug("Chat response: {}", response.substring(0, Math.min(100, response.length())));
        return response;
    }

    /**
     * 发送带系统提示的聊天消息
     *
     * @param systemPrompt 系统提示
     * @param userMessage  用户消息
     * @return AI 响应
     */
    public String chat(String systemPrompt, String userMessage) {
        log.debug("Chat with system prompt - Input: {}", userMessage);
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
        return response;
    }

    /**
     * 使用自定义 Prompt 发送消息
     *
     * @param prompt Prompt 对象
     * @return AI 响应
     */
    public String chat(Prompt prompt) {
        return chatClient.prompt(prompt).call().content();
    }
}
