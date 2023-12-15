package com.arextest.schedule.comparer;

import com.arextest.schedule.api.listener.ReplayResultListener;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReplayComparisonListener implements ReplayResultListener {
    @Resource
    private ExecutorService compareExecutorService;
    @Resource
    private ReplayResultComparer replayResultComparer;

    @Override
    public void notify(ReplayActionCaseItem caseItem) {
        compareExecutorService.execute(new AbstractTracedRunnable() {
            @Override
            protected void doWithTracedRunning() {
                boolean compareSuccess = replayResultComparer.compare(caseItem, true);
                if (!compareSuccess) {
                    LOGGER.error("Comparer returned false, retry, case id: {}", caseItem.getId());
                }
            }
        });
    }
}
