package org.dromara.docman.knowledge.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.knowledge.KnowledgeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 HTTP 客户端实现。
 * <p>
 * 设计意图：通过 HTTP 调用外部知识库服务（如 RAG 服务），支持文档检索增强。
 * 计划对接点：AI 生成插件的知识增强模式。
 * 当前状态：骨架实现，配置项已预留，尚未接入业务流程。
 */
@Slf4j
@Component
public class HttpKnowledgeClient implements KnowledgeClient {

    @Value("${docman.knowledge.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${docman.knowledge.timeout:30000}")
    private int timeout;

    @Override
    public List<KnowledgeResult> search(String query, int topK) {
        try {
            JSONObject body = new JSONObject();
            body.set("query", query);
            body.set("topK", topK);

            HttpResponse response = HttpRequest.post(baseUrl + "/knowledge/search")
                .body(body.toString())
                .contentType("application/json")
                .timeout(timeout)
                .execute();

            if (!response.isOk()) {
                log.error("知识库请求失败: status={}", response.getStatus());
                return List.of();
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray results = result.getJSONArray("results");
            if (results == null) {
                return List.of();
            }

            List<KnowledgeResult> list = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                list.add(new KnowledgeResult(item.getStr("content", ""), item.getStr("source", "")));
            }
            return list;
        } catch (Exception e) {
            log.error("知识库调用异常", e);
            return List.of();
        }
    }
}
