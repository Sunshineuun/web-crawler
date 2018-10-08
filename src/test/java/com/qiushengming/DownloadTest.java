package com.qiushengming;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DownloadTest {
  public static void main(String[] args) {
    long i = 1538105175000L;
    Date d = new Date(i);
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    String s = format.format(d);
    System.out.println(s);
  }

  public static void a(List<String> a) {
    for (String s : a) {
      System.out.println(a);
    }
  }
}
