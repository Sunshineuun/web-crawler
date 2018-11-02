package com.qiushengming.business.medlive;

import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import java.util.List;
import java.util.Map;

/**
 * 1. 按照科室采集，PDF文档
 */
public class Medlive1 extends BaseWebCrawler {

  @Override
  protected String getSiteName() {
    return "医脉通-指南PDF采集";
  }

  @Override
  protected List<URL> initURL() {
    return null;
  }

  @Override
  protected Response download(URL url) {
    return null;
  }

  @Override
  protected Boolean parser(Response r) {
    return null;
  }

  @Override
  protected List<Map<String, Object>> notice(List<Response> responses) {
    return null;
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    return null;
  }

  @Override
  protected String[] getTitles() {
    return new String[0];
  }
}
