package com.example.lab2.mapper;

import com.example.lab2.model.Entry;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EntryMapper {
  public Entry map(Map.Entry<String, Long> mapEntry) {
    return new Entry(mapEntry.getKey(), mapEntry.getValue());
  }
}
