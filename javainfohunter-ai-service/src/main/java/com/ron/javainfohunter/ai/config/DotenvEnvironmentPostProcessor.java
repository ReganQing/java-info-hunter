package com.ron.javainfohunter.ai.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * .env 文件环境后置处理器
 * <p>
 * 在 Spring Boot 启动时自动加载项目根目录的 .env 文件
 * 并将其中的环境变量注入到 Spring Environment 中
 * </p>
 *
 * <p>注册方式：</p>
 * 在 META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor 文件中注册
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 如果已经通过系统环境变量设置了 DASHSCOPE_API_KEY，跳过 .env 加载
        String existingApiKey = environment.getProperty("spring.ai.dashscope.api-key");
        if (existingApiKey != null && !existingApiKey.isEmpty()) {
            log.info("DASHSCOPE_API_KEY already set in environment, skipping .env loading");
            return;
        }

        try {
            Dotenv dotenv = loadDotenv();

            if (dotenv != null) {
                Map<String, Object> envMap = new HashMap<>();

                // 读取所有环境变量
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    envMap.put(key, value);

                    // 敏感信息脱敏后记录
                    if (isSensitive(key)) {
                        log.debug("Loaded from .env: {} = ***", key);
                    } else {
                        log.debug("Loaded from .env: {} = {}", key, value);
                    }
                });

                // 添加到 Spring Environment 优先级最高
                environment.getPropertySources()
                        .addFirst(new MapPropertySource(DOTENV_SOURCE_NAME, envMap));

                log.info(".env file loaded successfully with {} variables", envMap.size());

                // 验证关键环境变量
                if (envMap.containsKey("DASHSCOPE_API_KEY")) {
                    log.info("DASHSCOPE_API_KEY loaded from .env");
                }
            }

        } catch (DotenvException e) {
            log.warn("No .env file found or error reading .env: {}", e.getMessage());
            log.info("Environment variables will be loaded from system environment");
        }
    }

    /**
     * 尝试从多个位置加载 .env 文件
     */
    private Dotenv loadDotenv() {
        String[] possiblePaths = {
                ".",                      // 当前工作目录
                "../",                   // 父目录
                "../../",                // 祖父目录
                System.getProperty("user.dir"), // user.dir
        };

        for (String path : possiblePaths) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(path)
                        .filename(".env")
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();

                // 验证是否真的加载到了文件
                if (dotenv.entries().iterator().hasNext()) {
                    log.debug("Loaded .env from: {}", path);
                    return dotenv;
                }
            } catch (Exception e) {
                // 继续尝试下一个路径
            }
        }

        // 最后尝试不指定目录
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        if (dotenv.entries().iterator().hasNext()) {
            return dotenv;
        }
        return null;
    }

    /**
     * 判断是否为敏感信息
     */
    private boolean isSensitive(String key) {
        String upperKey = key.toUpperCase();
        return upperKey.contains("KEY")
                || upperKey.contains("SECRET")
                || upperKey.contains("PASSWORD")
                || upperKey.contains("TOKEN");
    }
}
