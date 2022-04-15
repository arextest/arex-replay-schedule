package io.arex.replay.schedule.dao;

import io.arex.replay.schedule.dao.handler.StringToHashMapHandler;
import io.arex.replay.schedule.model.ReplayActionCaseItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author jmo
 * @since 2021/9/23
 */

public interface ReplayActionCaseItemRepository extends RepositoryWriter<ReplayActionCaseItem> {

    @Insert("  INSERT INTO arex_replay_run_details (" +
            "  plan_item_id" +
            ", operation_id" +
            ", replay_dependence" +
            ", request_message_format" +
            ", request_message" +
            ", record_time" +
            ", send_status" +
            ", compare_status" +
            ", record_id" +
            ", source_case_id" +
            ", target_case_id" +
            ", consume_group" +
            ", request_headers" +
            ", request_method" +
            ", request_path" +
            " ) VALUES (" +
            "  #{planItemId}" +
            ", 0" +
            ", #{replayDependency}" +
            ", #{requestMessageFormat}" +
            ", #{requestMessage}" +
            ", #{recordTime}" +
            ", #{sendStatus}" +
            ", #{compareStatus}" +
            ", #{recordId}" +
            ", #{sourceResultId}" +
            ", #{targetResultId}" +
            ", #{consumeGroup}" +
            ", #{requestHeaders,jdbcType=OTHER,typeHandler=io.arex.replay.schedule.dao.handler" +
            ".StringToHashMapHandler}" +
            ", #{requestMethod}" +
            ", #{requestPath}" +
            ")")
    @Override
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id", before = false, resultType = Long.class)
    boolean save(ReplayActionCaseItem replayActionCaseItem);

    @Select("  SELECT  " +
            "  id" +
            ", plan_item_id" +
            ", operation_id" +
            ", replay_dependence AS replayDependency" +
            ", request_message_format" +
            ", request_message" +
            ", record_time" +
            ", send_status" +
            ", record_id" +
            ", source_case_id AS sourceResultId" +
            ", target_case_id AS targetResultId" +
            ", consume_group" +
            ", request_method" +
            ", request_headers AS requestHeaders" +
            ", request_path" +
            "  FROM arex_replay_run_details" +
            "  WHERE plan_item_id=#{planItemId}" +
            "  AND send_status=0" +
            "  ORDER BY id , replay_dependence" +
            "  LIMIT #{pageSize}"
    )
    @Results(id = "replay_run_details_result", value = {
             @Result(column = "requestHeaders", property = "requestHeaders", typeHandler =
                     StringToHashMapHandler.class)
    })
    List<ReplayActionCaseItem> waitingSendList(@Param("planItemId") long planItemId, @Param("pageSize") int pageSize);

    @Update("UPDATE arex_replay_run_details SET " +
            "  send_status=#{sendStatus}" +
            ", source_case_id=#{sourceResultId}" +
            ", target_case_id=#{targetResultId}" +
            "  WHERE id=#{id}")
    boolean updateSendResult(ReplayActionCaseItem replayActionCaseItem);

    @Update("UPDATE arex_replay_run_details SET compare_status=#{comparedStatus} WHERE id=#{id}")
    boolean updateCompareStatus(@Param("id") long id, @Param("comparedStatus") int comparedStatus);

    @Select("  SELECT  " +
            "  plan_item_id" +
            ", operation_id" +
            ", replay_dependence AS replayDependency" +
            ", request_message_format" +
            ", request_message" +
            ", record_time" +
            ", send_status" +
            ", record_id" +
            ", source_case_id AS sourceResultId" +
            ", target_case_id AS targetResultId" +
            ", consume_group" +
            ", request_path" +
            "  FROM arex_replay_run_details" +
            "  WHERE plan_item_id=#{planItemId}" +
            "  AND send_status=0" +
            "  ORDER BY id DESC" +
            "  LIMIT 1"
    )
    ReplayActionCaseItem lastOne(long planItemId);
}
