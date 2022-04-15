package io.arex.replay.schedule.model.deploy;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Data
public class DeploymentVersion {
    private String id;
    /**
     * example: APPROVED
     */
    private String status;
    private String name;
    /**
     * example 2021-08-26 14:35:59
     */
    @JsonAlias("created_at")
    private String createdAt;
    @JsonAlias("group_id")
    private String groupId;
    @JsonAlias("image_id")
    private String imageId;
    private DeploymentImage image;
}
