package com.arextest.schedule.comparer;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;

public interface CompareService {

  CompareResult compare(String baseMsg, String testMsg);

  CompareResult compare(String baseMsg, String testMsg, CompareOptions compareOptions);

  CompareResult quickCompare(String baseMsg, String testMsg);

  CompareResult quickCompare(String baseMsg, String testMsg, CompareOptions compareOptions);
}
