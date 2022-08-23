// import com.arextest.replay.schedule.WebSpringBootServletInitializer;
// import com.arextest.replay.schedule.dao.mongodb.ReplayPlanActionRepository;
// import com.arextest.replay.schedule.model.ReplayActionCaseItem;
// import com.arextest.replay.schedule.model.ReplayActionItem;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringRunner;
//
// import java.util.Date;
// import java.util.List;
//
// /**
//  * Created by rchen9 on 2022/8/22.
//  */
// @RunWith(SpringRunner.class)
// @ContextConfiguration(classes = {WebSpringBootServletInitializer.class})
// @SpringBootTest
// public class ReplayPlanActionRepositoryTest {
//
//     @Autowired
//     ReplayPlanActionRepository replayPlanActionRepository;
//
//     @Test
//     public void testSave() {
//
//         ReplayActionItem replayActionItem = new ReplayActionItem();
//         replayActionItem.setPlanId("630349c040e1105a5c512ef7");
//         replayActionItem.setOperationId("62fc90ee95146d322d5f21ed");
//         replayActionItem.setAppId("restapiApplication");
//         replayActionItem.setReplayStatus(4);
//         replayActionItem.setReplayBeginTime(new Date());
//         boolean save = replayPlanActionRepository.save(replayActionItem);
//         System.out.println();
//     }
//
//     @Test
//     public void testUpdate() {
//         ReplayActionItem replayActionItem = new ReplayActionItem();
//         replayActionItem.setId("63036705b1ee0a4948ffb245");
//         replayActionItem.setReplayStatus(4);
//         replayActionItem.setReplayCaseCount(10);
//         replayActionItem.setReplayBeginTime(new Date());
//         replayActionItem.setReplayFinishTime(new Date());
//         boolean update = replayPlanActionRepository.update(replayActionItem);
//         System.out.println();
//     }
//
//     @Test
//     public void testQueryPlanActionList() {
//         String planId = "630349c040e1105a5c512ef7";
//         List<ReplayActionItem> replayActionItems = replayPlanActionRepository.queryPlanActionList(planId);
//         System.out.println();
//     }
//
//     @Test
//     public void test(){
//         String appId = "restapiApplication";
//         long l = replayPlanActionRepository.queryRunningItemCount(appId);
//         System.out.println();
//     }
// }
