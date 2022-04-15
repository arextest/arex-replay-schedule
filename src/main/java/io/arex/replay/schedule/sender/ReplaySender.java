package io.arex.replay.schedule.sender;

import io.arex.replay.schedule.model.ReplayActionCaseItem;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplaySender {
    boolean isSupported(int sendType);

    boolean send(ReplayActionCaseItem caseItem);

    ReplaySendResult send(SenderParameters senderParameters);

    boolean prepareRemoteDependency(ReplayActionCaseItem caseItem);

    /**
     * 对于Java服务而言，发布后首次调用会进行即时Compilation，会耗费较多资源，此时直接高并发回放失败率较高
     * 此函数尝试发送一个不带ReplayID的CASE，用来触发掉此损耗
     */
    default boolean activeRemoteService(ReplayActionCaseItem caseItem) { return true; }
}
