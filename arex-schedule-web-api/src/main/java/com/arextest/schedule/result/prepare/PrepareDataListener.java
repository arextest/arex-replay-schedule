package com.arextest.schedule.result.prepare;


import com.arextest.schedule.api.listener.ReplayResultListener;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.replay.ReplayCaseContext;
import javax.annotation.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrepareDataListener implements ReplayResultListener {
    @Resource
    private PrepareDataLoader prepareDataLoader;

    @Override
    public void notify(ReplayActionCaseItem caseItem) {
        ReplayCaseContext context = new ReplayCaseContext();
        context.setReplayCaseResultList(prepareDataLoader.load(caseItem));
        caseItem.setReplayCaseContext(context);
    }
}
