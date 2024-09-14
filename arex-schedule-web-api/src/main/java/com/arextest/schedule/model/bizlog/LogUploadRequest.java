package com.arextest.schedule.model.bizlog;

import java.util.List;
import lombok.Data;

/**
 * @author: QizhengMo
 * @date: 2024/9/14 17:20
 */
@Data
public class LogUploadRequest {

  List<BizLog> logs;
}
