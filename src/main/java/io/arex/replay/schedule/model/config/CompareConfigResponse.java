package io.arex.replay.schedule.model.config;

import lombok.Data;

/**
 * Created by wang_yc on 2021/9/26
 */
@Data
public class CompareConfigResponse {
    private String errorMessage;
    private CompareConfigForReport compareConfig;

}
