package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

@OutboundPort("LLM 大模型生成服务")
public interface LlmGeneratePort {

    String generate(String prompt, int maxTokens);
}
