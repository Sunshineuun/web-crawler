package com.qiushengming.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
  /**
   * 获取当前时间，字符串格式
   * @return String
   */
  public static String nowDate(){
    return nowDate("YYYYMMddHHmm");
  }

  public static String nowDate(String pattern) {
    SimpleDateFormat format = new SimpleDateFormat(pattern);
    return format.format(new Date());
  }

  /**
   * d1 > d2
   * @param d1 日期1
   * @param d2 日期2
   * @param pattern 解析的格式
   * @return d1 > d2
   */
  public static Boolean compare(String d1, String d2, String pattern) {
    SimpleDateFormat format = new SimpleDateFormat(pattern);
    try {
      Date date1 = format.parse(d1);
      Date date2 = format.parse(d2);
      return date1.getTime() > date2.getTime();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Boolean.FALSE;
  }
}
