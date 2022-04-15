package io.arex.replay.schedule.model.jacoco;

import lombok.Data;

/**
 * Created by wang_yc on 2021/6/10
 */
@Data
public class CollectReportInfo {
    private Long id;
    private Long merge_request_iid;
    private Boolean status;

}
