package org.dromara.docman.infrastructure.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.docman.application.port.out.LlmGeneratePort;
import org.dromara.docman.config.DocmanAiConfig;

@Slf4j
@InfrastructureAdapter("LLM 大模型生成")
@RequiredArgsConstructor
public class HttpLlmGenerateAdapter implements LlmGeneratePort {

    private final DocmanAiConfig aiConfig;

    @Override
    public String generate(String prompt, int maxTokens) {
        JSONObject body = new JSONObject();
        body.set("model", aiConfig.getModel());
        body.set("prompt", prompt);
        body.set("stream", false);
        body.set("options", new JSONObject().set("num_predict", maxTokens));

        HttpResponse response = HttpRequest.post(aiConfig.getApiUrl())
            .body(body.toString())
            .contentType("application/json")
            .timeout(aiConfig.getTimeout())
            .execute();

        if (!response.isOk()) {
            log.error("AI API 请求失败: status={}", response.getStatus());
            return null;
        }
        JSONObject result = JSONUtil.parseObj(response.body());
        return result.getStr("response");
    }
}
