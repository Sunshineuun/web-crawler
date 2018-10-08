package com.qiushengming.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 1. 记录爬虫需要的相关配置
 * 2. 记录爬取到了那个URL，目的是为了达到增量，当前请求过的，不再使用。
 */
@Document(collection = "CrawlerConfig")
public class CrawlerConfig extends BaseEntity {

  @Field("URL")
  private URL url;
  @Field("CONFIG")
  private Map<String, Object> config = new HashMap<>();

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public Object get(String key) {
    return getConfig().get(key);
  }

  public void put(String key, Object value) {
    getConfig().put(key, value);
  }

  public void putAll(Map<String, Object> map) {
    getConfig().putAll(map);
  }
}
