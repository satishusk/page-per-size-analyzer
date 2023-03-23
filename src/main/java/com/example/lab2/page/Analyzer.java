package com.example.lab2.page;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
  private static final Pattern HREF_PATTERN;
  private static final Pattern PROTOCOL_DOMAIN_PATTERN;
  private final ExecutorService executorService;
  private final Phaser phaser;

  static {
    var hrefRegex = "href=\"([^\"]+)\"[\\s\\S]*?";
    HREF_PATTERN = Pattern.compile(hrefRegex);

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
    waitWhileAnyExecutorServiceThreadStart();
    phaser.arriveAndAwaitAdvance();
    log.info("End of analyze");
    return map;
  }

  private void waitWhileAnyExecutorServiceThreadStart() {
    while(phaser.getRegisteredParties() == 1);
  }

  private void recursiveAnalyze(int depth, String address, Map<String, Long> accumulateMap) {
    if (depth == -1) return;

    Collection<String> subUrls = new HashSet<>();
    try (var in = new URL(address).openConnection().getInputStream()) {
      depth--;
      subUrls = analyzeStream(in, address, accumulateMap);
    } catch (FileNotFoundException ignored) {
    } catch (IOException ex)  {
      log.error(ex.getMessage());
    }

    asyncHandlingForEach(depth, subUrls, accumulateMap);
  }

  private void asyncHandlingForEach(int depth, Collection<String> subAddresses, Map<String, Long> accumulateMap) {
    for (String subAddress : subAddresses) {
      executorService.execute(() -> {
        phaser.register();
        recursiveAnalyze(depth, subAddress, accumulateMap);
        phaser.arriveAndDeregister();
      });
    }
  }

  private Collection<String> analyzeStream(
    InputStream stream,
    String addressFrom,
    Map<String, Long> accumulateMap
  ) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String inputLine;
    long countBytes = 0;
    Collection<String> subAddresses = new HashSet<>();
    while ((inputLine = reader.readLine()) != null) {
      countBytes += inputLine.getBytes().length;
      Matcher hrefMatcher = HREF_PATTERN.matcher(inputLine);
      while(hrefMatcher.find()) {
        subAddresses.add(hrefMatcher.group(1));
      }
    }

    mapLoggedPut(accumulateMap, addressFrom, countBytes);
    return requiredNotPresentUrls(subAddresses, accumulateMap, addressFrom);
  }

  private void mapLoggedPut(Map<String, Long> accumulateMap, String key, Long value) {
    int sizeBefore = accumulateMap.size();
    accumulateMap.put(key, value);
    int sizeAfter = accumulateMap.size();
    if (sizeAfter != sizeBefore) {
      log.info("{}) {} - {}", sizeAfter, key, value);
    }
  }

  private Collection<String> requiredNotPresentUrls(
    Collection<String> subAddresses,
    Map<String, Long> accumulateMap,
    String addressFrom
  ) {
    return requiredHaveConcreteUrlPrefix(subAddresses, addressFrom).stream()
      .filter(s -> !accumulateMap.containsKey(s))
      .collect(Collectors.toSet());
  }

  private Collection<String> requiredHaveConcreteUrlPrefix(Collection<String> subAddresses, String urlPrefix) {
    String prefixPattern = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    Pattern urlPrefixPattern = Pattern.compile("(?i)\\b" + urlPrefix + "\\w*");

    var result = new HashSet<String>();
    for (String subAddress : subAddresses) {
      if (!subAddress.matches(prefixPattern)) {
        result.add(urlPrefix + (subAddress.charAt(0) == '/' ? subAddress.substring(1) : subAddress));
      } else if (urlPrefixPattern.matcher(subAddress).find()){
        result.add(subAddress);
      }
    }
    return result;
  }
}