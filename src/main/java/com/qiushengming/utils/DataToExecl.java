package com.qiushengming.utils;

import org.apache.http.client.utils.DateUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.sql.Clob;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component ("dataToExecl")
public class DataToExecl {
  public static SXSSFWorkbook createSXSSFWorkbook(int rowaccess) {
    return new SXSSFWorkbook(rowaccess);
  }

  public static SXSSFWorkbook createSXSSFWorkbook() {
    return new SXSSFWorkbook();
  }

  public static Sheet createSheet(Workbook wb) {
    return wb.createSheet();
  }

  private static int writeTitles(List<String> titles, Sheet s) {
    int dataStartRowNum = 1;

    CellStyle cellstyle = s.getWorkbook().createCellStyle();
    cellstyle.setAlignment(HorizontalAlignment.CENTER);
    cellstyle.setVerticalAlignment(VerticalAlignment.CENTER);

    int i = -1;
    for (String title : titles) {
      i++;
      // 写表头
      ExcelUtils.setContent(s, 0, i, CellType.STRING,title, cellstyle);
    }
    return dataStartRowNum;
  }

  public static Sheet writeData(List<String> titles, List<Map<String, Object>> records, Sheet s) {
    int indexRow = writeTitles(titles, s);
    // 遍历数据
    for (Map<String, Object> map : records) {
      int indexColumn = 0;
      for (String title : titles) {
          if (map.get(title) instanceof String) {
            ExcelUtils.setContent(s, indexRow, indexColumn, CellType.STRING,(String) map.get(title));
          } else if (map.get(title) instanceof Number) {
            ExcelUtils.setContent(s,indexRow,indexColumn,CellType.NUMERIC, String.valueOf(map.get(title)));
          } else if (map.get(title) instanceof Date) {
            ExcelUtils.setContent(s, indexRow, indexColumn,
                CellType.STRING, DateUtils.formatDate(((Date) map .get(title))));
          } else if (map.get(title) instanceof Clob) {
            ExcelUtils.setContent(s, indexRow, indexColumn,
                CellType.STRING, "大文本不予写入-");
          }
        indexColumn++;
        }
      indexRow++;
    }
    return s;
  }
}
