package com.arextest.schedule.model.dao.mongodb;

import java.util.Date;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;

/**
 * Created by rchen9 on 2022/8/9.
 */
@Data
@FieldNameConstants
public class ModelBase {

  @Id
  private String id;
  private Long dataChangeCreateTime;
  private Long dataChangeUpdateTime;
  private Date dataChangeCreateDate;
}