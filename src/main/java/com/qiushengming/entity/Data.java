package com.qiushengming.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "Data")
public class Data extends BaseEntity{
  private String responseId;
  @Field("DATA")
  private Map<String, Object> data = new HashMap<>();
  /**
   * 匹配到的关键字
   */
  @Field("KEY")
  private String key = "";

  public String getResponseId() {
    return responseId;
  }

  public void setResponseId(String responseId) {
    this.responseId = responseId;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public Object get(String key) {
    return getData().get(key);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
