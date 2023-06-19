package com.arextest.schedule.model.deploy;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Data
public class DeploymentEnvironment {
    private String id;
    /**
     * example  "fat36",
     */
    @JsonAlias("sub_env")
    private String subEnv;
    /**
     * example  "fat",
     */
    private String env;
    /**
     * example  "NTGXH",
     */
    private String region;
    @JsonAlias("ip")
    private String ipAddress;
    @JsonAlias("group_id")
    private String groupId;

    @JsonAlias("created_at")
    private String createdAt;
    @JsonAlias("application_id")
    private String appId;
    private DeploymentVersion version;

}