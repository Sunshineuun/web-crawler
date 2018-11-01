package com.qiushengming.entity;

import org.springframework.data.mongodb.core.mapping.Field;

public class NoticeHistory extends BaseEntity {
  @Field("DATA")
  private Data data;
  @Field("KEY")
  private String key;

  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
