package org.dromara.docman.service;

import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;

import java.util.List;

public interface IDocProcessConfigService {

    DocProcessConfig queryByInstanceId(Long instanceId);

    List<DocProcessConfig> listByStatus(DocProcessConfigStatus status);
}
