import com.arextest.replay.schedule.WebSpringBootServletInitializer;
import com.arextest.replay.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;

/**
 * Created by rchen9 on 2022/8/22.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {WebSpringBootServletInitializer.class})
@SpringBootTest
public class ReplayActionCaseItemRepositoryTest {


    @Autowired
    ReplayActionCaseItemRepository replayActionCaseItemRepository;

    @Test
    public void testSave() {
        ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
        replayActionCaseItem.setPlanItemId("6303698382a4c4e6b9b20d69");

        replayActionCaseItem.setRecordTime(System.currentTimeMillis());
        replayActionCaseItem.setSendStatus(1);
        replayActionCaseItem.setCompareStatus(0);
        replayActionCaseItem.setRecordId("AREX-10-5-122-70-5955270661210");
        replayActionCaseItem.setSourceResultId("");
        replayActionCaseItem.setTargetResultId("AREX-10-5-122-70-595527667980");
        replayActionCaseItem.setRequestHeaders(Collections.singletonMap("refer", "http://10.5.122.70:8080/"));
        replayActionCaseItem.setRequestMethod("GET");
        replayActionCaseItem.setRequestPath("/owners/find");

        boolean save = replayActionCaseItemRepository.save(replayActionCaseItem);
        System.out.println();
    }

    @Test
    public void testWaitingSendList() {
        String planItemId = "63036705b1ee0a4948ffb245";
        int pageSize = 10;
        List<ReplayActionCaseItem> replayActionCaseItems = replayActionCaseItemRepository.waitingSendList(planItemId, 10);
        System.out.println();
    }


    @Test
    public void testupdateSendResult() {
        ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
        replayActionCaseItem.setId("63036ddc82a4c4e6b9b20d6a");
        replayActionCaseItem.setSendStatus(0);
        replayActionCaseItem.setSourceResultId("AREX-10-5-122-70-595527667980");
        replayActionCaseItem.setTargetResultId("AREX-10-5-122-70-595527667980");
        boolean b = replayActionCaseItemRepository.updateSendResult(replayActionCaseItem);
        System.out.println();
    }

    @Test
    public void testUpdateCompareStatus() {
        ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
        replayActionCaseItem.setId("63036ddc82a4c4e6b9b20d6a");
        replayActionCaseItem.setCompareStatus(1);
        boolean b = replayActionCaseItemRepository.updateCompareStatus("63036ddc82a4c4e6b9b20d6a", 1);
        System.out.println();
    }

    @Test
    public void testLastOne(){
        String planItemId = "63036705b1ee0a4948ffb245";
        ReplayActionCaseItem replayActionCaseItem = replayActionCaseItemRepository.lastOne(planItemId);
        System.out.println();
    }


}

