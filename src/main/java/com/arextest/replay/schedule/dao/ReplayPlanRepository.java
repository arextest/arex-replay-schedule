package com.arextest.replay.schedule.dao;

import com.arextest.replay.schedule.model.ReplayPlan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.time.Duration;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/23
 */
public interface ReplayPlanRepository {
    @Insert("INSERT INTO arex_replay_plan (" +
            "  app_id" +
            ", plan_name" +
            ", target_image_id" +
            ", target_image_name" +
            ", source_env" +
            ", target_env" +
            ", source_host" +
            ", target_host" +
            ", case_source_type" +
            ", case_total_count" +
            ", case_source_from" +
            ", case_source_to" +
            ", plan_create_time" +
            ", plan_finish_time" +
            ", operator" +
            " ) VALUES (" +
            "  #{appId}" +
            ", #{planName}" +
            ", #{targetImageId}" +
            ", #{targetImageName}" +
            ", #{sourceEnv}" +
            ", #{targetEnv}" +
            ", #{sourceHost}" +
            ", #{targetHost}" +
            ", #{caseSourceType}" +
            ", #{caseTotalCount}" +
            ", #{caseSourceFrom}" +
            ", #{caseSourceTo}" +
            ", #{planCreateTime}" +
            ", #{planFinishTime}" +
            ", #{operator}" +
            ")")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = Long.class)
    boolean save(ReplayPlan replayPlan);

    @Update("UPDATE arex_replay_plan SET case_total_count=#{caseTotalCount} WHERE id=#{planId}")
    boolean updateCaseTotal(long planId, int caseTotal);

    @Update("UPDATE arex_replay_plan SET plan_finish_time=now() WHERE id=#{planId}")
    boolean finish(long planId);

    @Select("  SELECT " +
            "  app_id" +
            ", plan_name" +
            ", target_image_id" +
            ", target_image_name" +
            ", source_env" +
            ", target_env" +
            ", source_host" +
            ", target_host" +
            ", case_source_type" +
            ", case_total_count" +
            ", case_source_from" +
            ", case_source_to" +
            ", plan_create_time" +
            ", plan_finish_time" +
            ", operator" +
            "  FROM arex_replay_plan" +
            "  WHERE plan_finish_time is null " +
            "  AND plan_create_time BETWEEN #{offsetDuration} AND #{maxDuration}"
    )
    List<ReplayPlan> timeoutPlanList(@Param("offsetDuration") Duration offsetDuration,
                                     @Param("maxDuration") Duration maxDuration);
}
