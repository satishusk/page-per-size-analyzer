package com.example.lab2.page;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Component
public class Printer {
  public void printSummaryPagePerSize(Map<String, Long> pagePerSize) {
    System.out.println("-------------------Summary-------------------");
    printSizePerCount(pagePerSize.values());
    printMinMaxSizeAddresses(pagePerSize);
  }

  private void printSizePerCount(Collection<Long> pageSizes) {
    var count = pageSizes.size();
    var size = obtainPagesSize(pageSizes);
    System.out.println("Size: " + size + ", Count: " + count);
  }

  private Long obtainPagesSize(Collection<Long> pageSizes) {
    Long summarySize = 0L;
    for (Long pageSize : pageSizes) {
      summarySize += pageSize;
    }
    return summarySize;
  }

  private void printMinMaxSizeAddresses(Map<String, Long> pagePerSize) {
    var sortedPagePerSize = new ArrayList<>(pagePerSize.entrySet()).stream()
      .sorted(Map.Entry.comparingByValue())
      .toList();
    var min = sortedPagePerSize.get(0).getKey();
    var max = sortedPagePerSize.get(sortedPagePerSize.size() - 1).getKey();
    System.out.println("Min size page: " + min);
    System.out.println("Max size page: " + max);
  }
}
