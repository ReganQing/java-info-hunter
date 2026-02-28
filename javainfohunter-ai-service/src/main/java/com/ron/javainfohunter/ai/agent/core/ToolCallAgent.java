package com.ron.javainfohunter.ai.agent.core;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支持工具调用的 Agent
 * <p>
 * 集成 Spring AI Tool Callback 机制，实现：
 * 1. think(): 调用 LLM 决定是否使用工具
 * 2. act(): 执行 LLM 选择的工具
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ToolCallAgent extends ReActAgent {

    /**
     * 可用工具列表
     */
    private ToolCallback[] availableTools;

    /**
     * 工具调用响应
     */
    private ChatResponse toolCallResponse;

    /**
     * 工具调用管理器
     */
    private final ToolCallingManager toolCallingManager;

    /**
     * 聊天选项（禁用内置工具执行）
     */
    private final ChatOptions chatOptions;

    /**
     * 构造函数
     *
     * @param availableTools 可用工具列表
     */
    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();

        // 禁用 Spring AI 内置的工具执行，由我们手动管理上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        // 添加下一步提示
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessages().add(userMessage);
        }

        List<Message> messages = getMessages();
        Prompt prompt = new Prompt(messages, chatOptions);

        try {
            // 调用 LLM，获取工具调用决策
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallResponse = chatResponse;

            // 分析响应
            if (chatResponse != null && chatResponse.getResult() != null) {
                AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
                String result = assistantMessage.getText();
                List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

                log.info("{} 的思考：{}", getName(), result);
                log.info("{} 选择了 {} 个工具", getName(), toolCalls.size());

                String toolCallInfo = toolCalls.stream()
                        .map(toolCall -> String.format("工具：%s, 参数：%s",
                                toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                if (!toolCallInfo.isEmpty()) {
                    log.debug("{} 的 toolCallInfo：{}", getName(), toolCallInfo);
                }

                if (toolCalls.isEmpty()) {
                    // 没有工具调用，添加助手消息并结束
                    getMessages().add(assistantMessage);
                    return false;
                } else {
                    return true; // 需要执行工具
                }
            }
        } catch (Exception e) {
            log.error("{} 的思考出错：{}", getName(), e.getMessage());
            getMessages().add(new AssistantMessage("处理时遇到错误" + e.getMessage()));
            return false;
        }

        return false;
    }

    @Override
    public String act() {
        if (!toolCallResponse.hasToolCalls()) {
            return "没有调用工具";
        }

        try {
            // 执行工具调用
            Prompt prompt = new Prompt(getMessages(), chatOptions);
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
                    prompt, toolCallResponse);

            // 更新消息历史
            setMessages(toolExecutionResult.conversationHistory());

            // 处理工具响应
            List<Message> conversationHistory = toolExecutionResult.conversationHistory();
            if (!conversationHistory.isEmpty() &&
                conversationHistory.getLast() instanceof ToolResponseMessage toolResponseMessage) {

                // 提取工具执行结果
                String results = toolResponseMessage.getResponses().stream()
                        .filter(Objects::nonNull)
                        .map(response -> "工具 " + response.name() +
                                " 完成了任务！结果: " + response.responseData())
                        .collect(Collectors.joining("\n"));

                log.info("{} 的 act 结果：{}", getName(), results);

                // 检查是否调用了终止工具
                boolean hasTerminateTool = toolResponseMessage.getResponses().stream()
                        .anyMatch(response -> response != null &&
                                ("doTerminate".equals(response.name()) ||
                                 response.name().toLowerCase().contains("terminate")));

                if (hasTerminateTool) {
                    log.info("{} 已终止", getName());
                    setAgentState(AgentState.FINISHED);
                }

                return results;
            } else {
                log.warn("{}: 工具执行后未找到有效的 ToolResponseMessage", getName());
                return "工具执行完成，但未获取到有效响应";
            }
        } catch (Exception e) {
            log.error("{}: 工具执行过程中发生错误", getName(), e);
            return "工具执行失败：" + e.getMessage();
        }
    }

    @Override
    public void cleanup() {
        // 子类可重写此方法进行资源清理
    }
}
