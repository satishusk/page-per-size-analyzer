package com.example.lab2;

import com.example.lab2.page.InputProvider;
import com.example.lab2.page.Analyzer;
import com.example.lab2.page.Printer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Lab2Application {
  public static void main(String[] args) {
    var context = SpringApplication.run(Lab2Application.class, args);
    var inputProvider = context.getBean(InputProvider.class);
    var analyzer = context.getBean(Analyzer.class);
    var printer = context.getBean(Printer.class);

    var input = inputProvider.input();
    var pagePerSize = analyzer.pagesNamePerSize(input.getDepth(), input.getUri());
    printer.printSummaryPagePerSize(pagePerSize);
  }
}
