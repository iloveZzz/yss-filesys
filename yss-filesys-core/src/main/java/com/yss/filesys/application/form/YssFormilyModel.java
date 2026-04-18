package com.yss.filesys.application.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YssFormilyModel implements Serializable {
   private YssFormilyDsl.YssFormDefinition yssFormDefinition;
    Map<String, Object> initialValues;
}
