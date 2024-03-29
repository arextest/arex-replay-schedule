package com.arextest.schedule.service;


import com.arextest.schedule.model.ReplayActionItem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ReplayActionItemPreprocessServiceTest {

  @InjectMocks
  private ReplayActionItemPreprocessService service;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testIncludeFilter() throws Exception {
    Method filterMethod =
        ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class,
            Set.class);
    filterMethod.setAccessible(true);

    List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

    Set<String> includeOperations = new HashSet<>(2);
    includeOperations.add("operation2");
    includeOperations.add("*peration3");

    filterMethod.invoke(service, replayActionItemList, includeOperations, null);

    Assertions.assertEquals(2, replayActionItemList.size());
  }

  @Test
  public void testExcludeFilter() throws Exception {
    Method filterMethod =
        ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class,
            Set.class);
    filterMethod.setAccessible(true);

    List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

    Set<String> excludeOperations = new HashSet<>(2);
    excludeOperations.add("*peration2");
    excludeOperations.add("operation3");

    filterMethod.invoke(service, replayActionItemList, null, excludeOperations);

    Assertions.assertEquals(8, replayActionItemList.size());
  }

  @Test
  public void testIncludeAndExcludeFilter() throws Exception {
    Method filterMethod =
        ReplayActionItemPreprocessService.class.getDeclaredMethod("filter", List.class, Set.class,
            Set.class);
    filterMethod.setAccessible(true);

    List<ReplayActionItem> replayActionItemList = generateDemoReplayActionItems();

    Set<String> includeOperations = new HashSet<>(2);
    includeOperations.add("operation2");
    includeOperations.add("*peration3");

    Set<String> excludeOperations = new HashSet<>(2);
    excludeOperations.add("*peration7");
    excludeOperations.add("operation8");

    filterMethod.invoke(service, replayActionItemList, includeOperations, excludeOperations);
    Assertions.assertEquals(2, replayActionItemList.size());
    Assertions.assertEquals("operation2", replayActionItemList.get(0).getOperationName());
    Assertions.assertEquals("operation3", replayActionItemList.get(1).getOperationName());
  }


  private List<ReplayActionItem> generateDemoReplayActionItems() {
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