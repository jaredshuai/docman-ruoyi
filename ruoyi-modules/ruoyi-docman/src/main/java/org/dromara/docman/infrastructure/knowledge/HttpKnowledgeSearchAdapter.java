package org.dromara.docman.infrastructure.knowledge;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.docman.application.port.out.KnowledgeSearchPort;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@InfrastructureAdapter("RAG 知识检索")
public class HttpKnowledgeSearchAdapter implements KnowledgeSearchPort {

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
