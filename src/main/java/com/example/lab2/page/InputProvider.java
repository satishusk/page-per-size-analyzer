package com.example.lab2.page;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class InputProvider {
  public InputData input() {
    System.out.println("Variant: 3");

    try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
      System.out.print("InputData depth: ");
      int depth = Integer.parseInt(br.readLine());

      System.out.print("InputData ip address and port (address:<optional port>): ");
      String uri = br.readLine();

      return new InputData(uri, depth);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
