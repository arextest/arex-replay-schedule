package com.arextest.schedule.model.noiseidentify;

import java.util.List;
import java.util.Map;

import com.arextest.diff.model.log.NodeEntity;
import com.arextest.schedule.model.ReplayCompareResult;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplayNoiseItemDto {

    private List<NodeEntity> nodePath;

    private String compareResultId;
    private ReplayCompareResult compareResult;
    private List<Integer> logIndexes;

    // 从属于该path的错误, 以及错误数
    private Map<String, Integer> subPaths;

    // 错误的mocker数
    private int caseCount;

    public int getPathCount() {
        return subPaths.size();
    }
}