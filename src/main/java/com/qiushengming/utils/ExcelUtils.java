package com.qiushengming.utils;

import com.qiushengming.execptions.ReadExcelException;
import org.apache.http.client.utils.DateUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ExcelUtils {
  public static final String TYPE_STRING = "string";

  private static final String TYPE_NUMERIC = "numeric";

  private static final String TYPE_DATE = "date";

  public static boolean isXLS(File file) {
    String name =
        file.getName().substring(file.getName().lastIndexOf(".") + 1);
    return name.equalsIgnoreCase("xls");

  }

  public static int getColnum(XSSFSheet sheet) throws ReadExcelException {
    try {
      XSSFRow row = sheet.getRow(0);
      return row.getLastCellNum();
    }
    catch (Exception e) {
      throw new ReadExcelException(e);
    }
  }

  /**
   *
   * @date 2017年4月14日 上午11:11:56
   * @author qiushengming
   * @param sheet 当前sheet页
   * @param rownum 当前行
   * @param colnum 当前列
   * @param cType XSSFCell类型
   * @param value 写入值
   * <p>因cellStyle参数的引入，为兼容以前的方法，设置该方法</p>
   */
  public static void setContent(Sheet sheet, int rownum, int colnum,
                                CellType cType, String value) {
    setContent(sheet, rownum, colnum, cType, value, null);
  }

  /**
   *
   * @date 2017年4月14日 上午11:10:14
   * @author qiushengming
   * @param sheet 当前sheet页面
   * @param rownum 当前行
   * @param colnum 当前列
   * @param cType XSSFCell类型
   * @param value 写入值
   * @param cellStyle 单元格样式
   * <p>cellStyle参数应kbms2.2版本需求导出需要增加</p>
   */
  public static void setContent(Sheet sheet, int rownum, int colnum,
                                CellType cType, String value, CellStyle cellStyle) {
    Row row;
    Cell cell;

    row = sheet.getRow(rownum);
    if (row == null) {
      row = sheet.createRow(rownum);
    }

    cell = row.getCell((short) colnum);
    if (cell == null) {
      cell = row.createCell((short) colnum);
    }

    if (cellStyle != null) {
      cell.setCellStyle(cellStyle);
    }

    cell.setCellType(cType);
    switch (cType) {
      case NUMERIC:
        cell.setCellValue(Double.parseDouble(value));
        break;
      case STRING:
        cell.setCellValue(new XSSFRichTextString(value));
        break;
      case FORMULA:
        cell.setCellFormula(value);
        break;
    }
  }

  public static String getFormula(XSSFSheet sheet, int rownum, int colnum)
      throws ReadExcelException {
    XSSFRow row;
    XSSFCell cell;

    try {
      row = sheet.getRow(rownum);
      if (row != null) {
        cell = row.getCell(colnum);

        if (cell != null) {
          if (cell.getCellTypeEnum() == CellType.FORMULA) {
            return cell.getCellFormula();
          }
        }
      }

      return "";
    }
    catch (Exception e) {
      throw new ReadExcelException(e);
    }
  }

  public static XSSFCell getCell(XSSFSheet sheet, int rownum, int colnum)
      throws ReadExcelException {
    XSSFRow row;
    XSSFCell cell;

    try {
      row = sheet.getRow(rownum);
      if (row != null) {
        cell = row.getCell(colnum);
        return cell;
      }

      return null;
    }
    catch (Exception e) {
      throw new ReadExcelException(e);
    }

  }

  public static String getContent(XSSFSheet sheet, int rownum, int colnum)
      throws ReadExcelException {
    XSSFRow row;
    XSSFCell cell;
    XSSFRichTextString text;
    java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
    nf.setGroupingUsed(false);

    try {
      row = sheet.getRow(rownum);
      if (row != null) {
        cell = row.getCell(colnum);

        if (cell != null) {
          CellType cType = cell.getCellTypeEnum();
          switch (cType) {
            case NUMERIC:
              if (DateUtil.isCellDateFormatted(cell)) {
                return DateUtils
                    .formatDate(DateUtil.getJavaDate(
                        cell.getNumericCellValue()));
              }
              else {
                return nf.format(cell.getNumericCellValue());
              }
            case STRING:
              text = cell.getRichStringCellValue();
              return text.getString().trim();
            case FORMULA:
              sheet.setDisplayFormulas(true);
              return nf.format(cell.getNumericCellValue());
            case BLANK:
              return "";
          }
        }
      }

      return null;
    }
    catch (Exception e) {
      throw new ReadExcelException(e);
    }
  }

  public static Object getContent(Sheet sheet, int rownum, int colnum, String type)
      throws ReadExcelException {
    Row row;
    Cell cell;
    RichTextString text;
    try {
      row = sheet.getRow(rownum);
      if (row != null) {
        cell = row.getCell(colnum);

        if (cell != null) {
          CellType cType = cell.getCellTypeEnum();
          switch (cType) {
            case NUMERIC:
              java.text.NumberFormat nf =
                  java.text.NumberFormat.getInstance();
              nf.setGroupingUsed(false);
              if (type.equals(ExcelUtils.TYPE_DATE)) {
                return cell.getDateCellValue();
              }
              else {
                return nf.format(cell.getNumericCellValue());
              }
            case STRING:
              text = cell.getRichStringCellValue();
              return text.getString().trim();
            case FORMULA:
              /*
               * 此处为公式处理。 可能出现非纯数值的状况，当为非纯数值的状况下会抛出异常，
               * 在内部自行处理，改为文本转换。
               */
              String value;
              java.text.NumberFormat nf2 =
                  java.text.NumberFormat.getInstance();
              nf2.setGroupingUsed(false);
              sheet.setDisplayFormulas(true);
              try {
                value = nf2.format(cell.getNumericCellValue());
              }
              catch (Exception e) {
                value = String
                    .valueOf(cell.getRichStringCellValue());
              }
              return value;
            // return Double.toString(cell.getNumericCellValue());
            case BLANK:
              if (type.equals(ExcelUtils.TYPE_NUMERIC)) {
                return "0";
              }
              return "";
          }
        }
        else {
          if (type.equals(ExcelUtils.TYPE_NUMERIC)) {
            return "0";
          }
          else {
            return "";
          }
        }
      }
      return "";
    }
    catch (Exception e) {
      throw new ReadExcelException(e);
    }
  }

  public static List<String> getTitle(XSSFSheet sheet, int startrow, int startcol)
      throws ReadExcelException {
    List<String> titleList = new ArrayList<>();
    int colnum = getColnum(sheet);

    for (int i = startcol; i < colnum; i++) {
      titleList.add(ExcelUtils.getContent(sheet, startrow, i));
    }
    return titleList;
  }

}