package io.arex.replay.schedule.model.deploy;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Data
public class DeploymentImage {
    private String id;
    private String name;

    /**
     * The author of source code
     */
    private String creator;

    /**
     * from the latest of git push commit message
     */
    private String note;
    @JsonAlias("created_at")
    private String createdAt;
}
