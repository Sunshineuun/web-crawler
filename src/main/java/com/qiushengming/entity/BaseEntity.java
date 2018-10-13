package com.qiushengming.entity;

import com.google.gson.Gson;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BaseEntity
    implements Serializable {

  @Transient
  private static final Gson GSON = new Gson();

  @Id
  @Indexed
  private String id = UUID.randomUUID().toString();
  /**
   * 因为是多个站点存储在一起的，所以要以它为标识
   */
  @Field("TYPE")
  private String type = "-";
  /**
   * 备注
   */
  @Field("REMARK")
  private String remark;
  /**
   * 更新时间 <br>
   * 默认值 - new Date()
   */
  @Field("UPDATE_TIME")
  private Date updateTime = new Date();
  /**
   * 创建时间 <br>
   * 默认值 - new Date()
   */
  @Field("CREATE_TIME")
  private Date createTime = new Date();
  /**
   * 是否有效；有效-1，无效-0,其它状态临行定义 <br>
   * 默认值 - 1
   */
  @Field("IS_ENABLE")
  private int isEnable = 1;
  /**
   * 其它一些信息的存储 <br>
   * 默认值 - HashMap
   */
  @Field("OTHER_INFO")
  private Map<String, Object> otherInfo =  new HashMap<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public int getIsEnable() {
    return isEnable;
  }

  public void setIsEnable(int isEnable) {
    this.isEnable = isEnable;
  }

  public Map<String, Object> getOtherInfo() {
    return otherInfo;
  }

  public void setOtherInfo(Map<String, Object> otherInfo) {
    this.otherInfo = otherInfo;
  }

  @Override
  public String toString() {
    return GSON.toJson(this);
  }
}
