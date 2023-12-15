package com.arextest.schedule.model.replay;

import java.util.List;
import lombok.Data;

@Data
public class ReplayCaseResult {
    private String categoryName;
    private List<ReplayCaseItem> recordCaseList;
    private List<ReplayCaseItem> replayCaseList;
}

