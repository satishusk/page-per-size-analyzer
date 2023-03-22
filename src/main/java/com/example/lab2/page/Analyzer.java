package com.example.lab2.page;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Analyzer {
  private static final Pattern LINK_PATTERN;
  private static final Pattern PROTOCOL_DOMAIN_PATTERN;
  private final ExecutorService executorService;
  private final Phaser phaser;

  static {
    var linkRegex = "href=\"([^\"]+)\"[\\s\\S]*?";
    LINK_PATTERN = Pattern.compile(linkRegex);

    var protocolDomainRegex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#%?=~_|!:,.;]*[-a-zA-Z0-9+&@#%=~_|]";
    PROTOCOL_DOMAIN_PATTERN = Pattern.compile(protocolDomainRegex);
  }

  public Analyzer() {
    var availableProcessors = Runtime.getRuntime().availableProcessors();
    executorService = Executors.newFixedThreadPool(availableProcessors);

    phaser = new Phaser(1);
  }

  public Map<String, Long> pagesNamePerSize(int depth, String address) {
    var map = new ConcurrentHashMap<String, Long>();
    recursiveAnalyze(depth, address, map);
    phaser.arriveAndAwaitAdvance();
    executorService.shutdown();
    return map;
  }

  private void recursiveAnalyze(int depth, String address, Map<String, Long> accumulateMap) {
    if (depth == 1) return;

    Collection<String> subUrls = new HashSet<>();
    try (var in = new URL(address).openConnection().getInputStream()) {
      depth--;
      subUrls = analyzeStream(in, address, accumulateMap);
    } catch (BrokenBarrierException | InterruptedException | IOException ex)  {
      log.error(ex.getMessage());
    }
    asyncHandlingForEach(depth, subUrls, accumulateMap);
  }

  private Collection<String> analyzeStream(
    InputStream stream,
    String addressFrom,
    Map<String, Long> accumulateMap
  ) throws IOException, BrokenBarrierException, InterruptedException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String inputLine;
    long countBytes = 0;
    Collection<String> subUrls = new HashSet<>();
    while ((inputLine = reader.readLine()) != null) {
      countBytes += inputLine.getBytes().length;
      Matcher linkMatcher = LINK_PATTERN.matcher(inputLine);
      while(linkMatcher.find()) {
        subUrls.add(linkMatcher.group(1));
      }
    }

    mapLoggedPut(accumulateMap, addressFrom, countBytes);
    return requiredNotPresentUrls(subUrls, accumulateMap, addressFrom);
  }

  private void mapLoggedPut(Map<String, Long> accumulateMap, String key, Long value) {
    int sizeBefore = accumulateMap.size();
    accumulateMap.put(key, value);
    int sizeAfter = accumulateMap.size();
    if (sizeAfter != sizeBefore) {
      log.info("{}) {} - {}", sizeAfter, key, value);
    }
  }

  private Collection<String> requiredNotPresentUrls(Collection<String> subUrls, Map<String, Long> accumulateMap, String addressFrom) {
    return requiredHaveConcreteUrlPrefix(subUrls, addressFrom).stream()
      .filter(s -> !accumulateMap.containsKey(s))
      .collect(Collectors.toSet());
  }

  private Collection<String> requiredHaveConcreteUrlPrefix(Collection<String> urls, String urlPrefix) {
    String prefixPattern = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    Pattern urlPrefixPattern = Pattern.compile("(?i)\\b" + urlPrefix + "/\\w*");

    var result = new HashSet<String>();
    for (String url : urls) {
      if (!url.matches(prefixPattern)) {
        result.add(urlPrefix + (url.charAt(0) != '/' ? "/" : "" ) + url);
      } else if (urlPrefixPattern.matcher(url).find()){
        result.add(url);
      }
    }
    return result;
  }

  private void asyncHandlingForEach(int depth, Collection<String> subUrls, Map<String, Long> accumulateMap) {
    for (String subUrl : subUrls) {
      phaser.register();
      executorService.execute(() -> {
        recursiveAnalyze(depth, subUrl, accumulateMap);
        phaser.arriveAndDeregister();
      });
    }
  }
}