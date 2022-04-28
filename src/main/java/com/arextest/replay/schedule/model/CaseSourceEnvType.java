package com.arextest.replay.schedule.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/9/22
 */
public enum CaseSourceEnvType {

    /**
     * 生产：0
     */
    PRO(0),

    /**
     * 测试：1
     */
    TEST(1);
    @Getter
    private final int value;

    CaseSourceEnvType(int value) {
        this.value = value;
    }

    public static CaseSourceEnvType toCaseSourceType(int caseSourceType) {
        if (caseSourceType == CaseSourceEnvType.PRO.getValue()) {
            return CaseSourceEnvType.PRO;
        }
        if (caseSourceType == CaseSourceEnvType.TEST.getValue()) {
            return CaseSourceEnvType.TEST;
        }
        return null;
    }
}
