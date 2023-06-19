package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.web.boot.WebSpringBootServletInitializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = WebSpringBootServletInitializer.class)
@RunWith(SpringRunner.class)
public class ReplayActionItemPreprocessServiceTest {

    @Resource
    private ReplayActionItemPreprocessService service = new ReplayActionItemPreprocessService();
    @Test
    public void testIncludeFilter() throws Exception {
        Method filterMethod =
                ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class, Set.class);
        filterMethod.setAccessible(true);

        List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

        Set<String> includeOperations = new HashSet<>(2);
        includeOperations.add("operation2");
        includeOperations.add("*peration3");

        filterMethod.invoke(service, replayActionItemList, includeOperations, null);

        assertEquals(2, replayActionItemList.size());
    }

    @Test
    public void testExcludeFilter() throws Exception{
        Method filterMethod =
                ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class, Set.class);
        filterMethod.setAccessible(true);

        List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

        Set<String> excludeOperations = new HashSet<>(2);
        excludeOperations.add("*peration2");
        excludeOperations.add("operation3");

        filterMethod.invoke(service, replayActionItemList, null, excludeOperations);

        assertEquals(8, replayActionItemList.size());
    }

    @Test
    public void testIncludeAndExcludeFilter() throws Exception{
        Method filterMethod =
                ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class, Set.class);
        filterMethod.setAccessible(true);

        List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

        Set<String> includeOperations = new HashSet<>(2);
        includeOperations.add("operation2");
        includeOperations.add("*peration3");

        Set<String> excludeOperations = new HashSet<>(2);
        excludeOperations.add("*peration7");
        excludeOperations.add("operation8");

        filterMethod.invoke(service, replayActionItemList, includeOperations, excludeOperations);
        assertEquals(2, replayActionItemList.size());
        assertEquals("operation2", replayActionItemList.get(0).getOperationName());
        assertEquals("operation3", replayActionItemList.get(1).getOperationName());
    }


    private List<ReplayActionItem> generateDemoReplayActionItems(){
        List<ReplayActionItem> replayActionItemList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ReplayActionItem item = new ReplayActionItem();
            item.setServiceName("service" + i);
            item.setOperationName("operation" + i);
            replayActionItemList.add(item);
        }
        return replayActionItemList;
    }
}