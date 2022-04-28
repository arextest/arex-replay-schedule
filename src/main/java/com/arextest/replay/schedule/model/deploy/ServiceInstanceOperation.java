package com.arextest.replay.schedule.model.deploy;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/26
 */
@Data
public class ServiceInstanceOperation {
    @JsonAlias("Name")
    private String name;
    @JsonAlias("RequestMessage")
    private DefinedMessageFormatter requestMessage;
    @JsonAlias("ResponseMessage")
    private DefinedMessageFormatter responseMessage;
}
