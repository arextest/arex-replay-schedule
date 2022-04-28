package com.arextest.replay.schedule.dao;

import com.arextest.replay.schedule.model.ReplayActionItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author jmo
 * @since 2021/9/23
 */
public interface ReplayPlanActionRepository extends RepositoryWriter<ReplayActionItem> {

    @Insert("INSERT INTO arex_replay_plan_item (" +
            "  plan_id" +
            ", operation_id" +
            ", app_id" +
            ", replay_status" +
            ", replay_begin_time" +
            ", replay_finish_time" +
            ", replay_case_count" +
            " ) VALUES (" +
            "  #{planId}" +
            ", #{operationId}" +
            ", #{appId}" +
            ", #{replayStatus}" +
            ", #{replayBeginTime}" +
            ", #{replayFinishTime}" +
            ", #{replayCaseCount}" +
            ")")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = Long.class)
    boolean save(ReplayActionItem actionItem);

    @Update("UPDATE arex_replay_plan_item SET" +
            "  replay_status= #{replayStatus}" +
            ", replay_begin_time= #{replayBeginTime}" +
            ", replay_finish_time=#{replayFinishTime}" +
            ", replay_case_count=#{replayCaseCount}" +
            " WHERE id=#{id}")
    boolean update(ReplayActionItem actionItem);

    @Select("  SELECT " +
            "  id" +
            ", plan_id" +
            ", operation_id" +
            ", replay_status" +
            ", replay_begin_time" +
            ", replay_finish_time" +
            ", replay_case_count" +
            "  FROM arex_replay_plan_item" +
            "  WHERE plan_id=#{planId}"
    )
    List<ReplayActionItem> queryPlanActionList(long planId);

    @Select("  SELECT COUNT(*)" +
            "  FROM arex_replay_plan_item" +
            "  WHERE app_id=#{appId} AND  replay_status in(0,1)"
    )
    int queryRunningItemCount(String appId);

}
