package com.arextest.schedule.model.noiseidentify;

import com.arextest.schedule.model.ReplayActionCaseItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemForNoiseIdentify {

  private String planId;
  private String planItemId;
  private String contextName;
  private List<ReplayActionCaseItem> cases;
}