package com.arextest.schedule.model;

import lombok.Data;

/**
 * Created by wang_yc on 2021/9/15
 */
@Data
public class CommonResponse {
    private Integer result;
    private String desc;
    private Object data;

    public CommonResponse() {

    }

    public CommonResponse(Integer result, String desc) {
        this.result = result;
        this.desc = desc;
    }

    public CommonResponse(Integer result, String desc, Object data) {
        this.result = result;
        this.desc = desc;
        this.data = data;
    }

    public static CommonResponse successResponse(String desc, Object data) {
        return new CommonResponse(1, desc, data);
    }

    public static CommonResponse badResponse(String desc) {
        return new CommonResponse(2, desc);
    }

    public static CommonResponse badResponse(String desc, Object data) {
        return new CommonResponse(2, desc, data);
    }
}