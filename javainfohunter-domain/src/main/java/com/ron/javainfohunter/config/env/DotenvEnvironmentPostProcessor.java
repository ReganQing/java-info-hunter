package com.ron.javainfohunter.config.env;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        try {
            Dotenv dotenv = loadDotenv();

            if (dotenv != null) {
                Map<String, Object> envMap = new HashMap<>();

                // 只将 .env 中有、但系统环境变量中没有的 key 加入
                // 系统环境变量优先级更高，不覆盖已存在的值
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // 如果系统环境变量中已经有该 key，则跳过（不覆盖）
                    if (System.getenv(key) != null) {
                        log.debug("Skipping .env key already set in system env: {}", key);
                        return;
                    }

                    envMap.put(key, value);

                    if (isSensitive(key)) {
                        log.debug("Loaded from .env: {} = ***", key);
                    } else {
                        log.debug("Loaded from .env: {} = {}", key, value);
                    }
                });

                // 添加到 Spring Environment，优先级低于系统环境变量
                environment.getPropertySources()
                        .addLast(new MapPropertySource(DOTENV_SOURCE_NAME, envMap));

                log.info(".env file loaded successfully with {} variables", envMap.size());

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
     * <p>
     * 搜索顺序：从 user.dir 开始逐级向上查找，直到找到包含 .env 的目录。
     * 这样无论从项目根目录还是子模块目录启动，都能正确定位 .env 文件。
     * </p>
     */
    private Dotenv loadDotenv() {
        List<String> searchPaths = buildSearchPaths();

        for (String path : searchPaths) {
            try {
                File envFile = new File(path, ".env");
                if (!envFile.exists() || !envFile.isFile()) {
                    continue;
                }

                Dotenv dotenv = Dotenv.configure()
                        .directory(path)
                        .filename(".env")
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();

                // 验证是否真的加载到了内容
                if (dotenv.entries().iterator().hasNext()) {
                    log.info("Loaded .env from: {}", envFile.getAbsolutePath());
                    return dotenv;
                }
            } catch (Exception e) {
                log.debug("Could not load .env from {}: {}", path, e.getMessage());
            }
        }

        log.warn("No .env file found in any search path: {}", searchPaths);
        return null;
    }

    /**
     * 构建 .env 搜索路径列表：从 user.dir 开始，逐级向上遍历父目录
     */
    private List<String> buildSearchPaths() {
        List<String> paths = new ArrayList<>();

        // 从 user.dir 开始向上查找（最多向上 4 层）
        File dir = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        for (int i = 0; i < 5 && dir != null; i++) {
            paths.add(dir.getAbsolutePath());
            dir = dir.getParentFile();
        }

        return paths;
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
