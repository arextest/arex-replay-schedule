// import com.arextest.replay.schedule.WebSpringBootServletInitializer;
// import com.arextest.replay.schedule.dao.mongodb.ReplayPlanRepository;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringRunner;
//
// import javax.annotation.Resource;
//
// @RunWith(SpringRunner.class)
// @ContextConfiguration(classes = {WebSpringBootServletInitializer.class})
// @SpringBootTest
// public class ReplayPlanRepositoryTest {
//
//     @Resource
//     ReplayPlanRepository replayPlanRepository;
//
//     // @Test
//     // public void testPlanId() {
//     //     String planId = "6302ed742b06b560fb77cf82";
//     //     List<ReplayPlan> replayPlans = replayPlanRepository.queryAll(planId);
//     //     ReplayPlan replayPlan = replayPlans.get(0);
//     //     replayPlan.setPlanCreateTime(new Date());
//     //     replayPlan.setPlanFinishTime(null);
//     //     boolean save = replayPlanRepository.save(replayPlan);
//     //     System.out.println();
//     // }
//
//     @Test
//     public void testUpdateCaseTotal() {
//         String planId = "630349c040e1105a5c512ef7";
//         int caseTotal = 100;
//         boolean b = replayPlanRepository.updateCaseTotal(planId, caseTotal);
//         System.out.println();
//     }
//
//     @Test
//     public void testFinish() {
//         String planId = "630349c040e1105a5c512ef7";
//         boolean b = replayPlanRepository.finish(planId);
//         System.out.println();
//     }
//
// }