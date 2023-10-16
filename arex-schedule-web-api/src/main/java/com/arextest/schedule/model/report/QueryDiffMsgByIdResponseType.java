package com.arextest.schedule.model.report;

import com.arextest.schedule.model.DesensitizationResponseType;
import lombok.Data;

/**
 * Created by qzmo on 2023/06/27.
 */
@Data
public class QueryDiffMsgByIdResponseType extends DesensitizationResponseType {
    CompareResultDetail compareResultDetail;
}