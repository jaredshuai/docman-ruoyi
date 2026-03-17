package org.dromara.docman.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDef {
    private String name;
    private String type;
    private boolean required;
    private String description;
}
