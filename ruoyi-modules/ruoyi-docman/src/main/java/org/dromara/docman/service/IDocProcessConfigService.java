package org.dromara.docman.service;

import org.dromara.docman.domain.entity.DocProcessConfig;

public interface IDocProcessConfigService {

    DocProcessConfig queryByInstanceId(Long instanceId);
}
