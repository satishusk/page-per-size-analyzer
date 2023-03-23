package com.example.lab2.controller;

import com.example.lab2.mapper.EntryMapper;
import com.example.lab2.page.Analyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class AnalyzeController {
  private final Analyzer analyzer;
  private final EntryMapper entryMapper;

  @GetMapping("/analyze")
  public String analyzePage() {
    return "analyze";
  }

  @PostMapping("/analyze")
  public String analyze(
    @RequestParam("depth") Integer depth,
    @RequestParam("uri") String uri,
    Model model
  ) {
    var pagesNamePerSize = analyzer.pagesNamePerSize(depth, uri);
    var pageSizes = pagesNamePerSize.values();
    var sortedEntries = sortEntries(pagesNamePerSize);

    model.addAttribute("depth", depth);
    model.addAttribute("uri", uri);
    model.addAttribute("count", pageSizes.size());
    model.addAttribute("summarySize", obtainPagesSize(pageSizes));
    model.addAttribute("min", sortedEntries.get(0).getKey());
    model.addAttribute("minBytes", sortedEntries.get(0).getValue());
    model.addAttribute("max", sortedEntries.get(sortedEntries.size() - 1).getKey());
    model.addAttribute("maxBytes", sortedEntries.get(sortedEntries.size() - 1).getValue());
    model.addAttribute("entries", pagesNamePerSize.entrySet().stream().map(entryMapper::map).toList());
    return "analyze";
  }

  private Long obtainPagesSize(Collection<Long> pageSizes) {
    var summarySize = 0L;
    for (Long pageSize : pageSizes) {
      summarySize += pageSize;
    }
    return summarySize;
  }

  private List<Map.Entry<String, Long>> sortEntries(Map<String, Long> pagePerSize) {
    return new ArrayList<>(pagePerSize.entrySet()).stream()
      .sorted(Map.Entry.comparingByValue())
      .toList();
  }
}
