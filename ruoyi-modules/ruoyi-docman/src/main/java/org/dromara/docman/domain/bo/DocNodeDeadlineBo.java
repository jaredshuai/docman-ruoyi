package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocNodeDeadlineBo {

    @NotNull(message = "ID不能为空")
    private Long id;

    private LocalDate deadline;

    private Integer durationDays;
}
