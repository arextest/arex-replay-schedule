package com.arextest.schedule.model.storage;

import lombok.Data;

import java.util.List;

/**
 * @author qzmo
 * @since 2023/09/19
 */
@Data
public class PostProcessResultRequestType {
    private List<ResultCodeGroup> results;
}