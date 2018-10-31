package com.qiushengming.entity;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "URL")
public class URL
    extends BaseEntity
    implements Serializable {
  /**
   * URL,资源地址
   */
  @Indexed
  @Field("URL")
  private String url;

  private String charset = "UTF-8";
  /**
   * 层级深度；由数字组成，从0往后，数值越大深度越深 <br>
   * 默认值 - 0
   */
  @Field("TREE")
  private int tree = 0;
  /**
   * 使用上限 <br>
   * 默认值 - 0,上限次数由其它来限定
   */
  @Field("USE_LIMIT")
  private int uselimit = 0;

  /**
   * 请求参数 <br>
   * 默认值 - 空的HashMap
   */
  @Field("PARAMS")
  private Map<String, Object> params = new HashMap<>();

  /**
   * 请求头 - <br>
   * 默认值 - 空的HashMap
   */
  @Field("HEADERS")
  private Map<String, String> headers = new HashMap<>();

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getTree() {
    return tree;
  }

  public void setTree(int tree) {
    this.tree = tree;
  }

  public int getUselimit() {
    return uselimit;
  }

  public void setUselimit(int uselimit) {
    this.uselimit = uselimit;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public void putParamsKey(String key, Object value) {
    params.put(key, value);
  }

  public void removeParamsKey(String key) {
    params.remove(key);
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }
}
