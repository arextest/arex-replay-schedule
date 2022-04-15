package io.arex.replay.schedule.model;

import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Data
public class AppServiceOperationDescriptor {
    private long id;
    private AppServiceDescriptor parent;
    private String operationName;
    private int status;
    private int operationType;
}
