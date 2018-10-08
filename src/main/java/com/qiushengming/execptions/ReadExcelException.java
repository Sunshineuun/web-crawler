package com.qiushengming.execptions;

public class ReadExcelException extends RuntimeException {
  private static final long serialVersionUID = -3909738823399671238L;

  public ReadExcelException(){

  }

  public ReadExcelException(Exception e){
    super(e);
  }

  public ReadExcelException(String msg){
    super(msg);
  }
}
