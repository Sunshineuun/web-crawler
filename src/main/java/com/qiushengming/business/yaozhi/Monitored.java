package com.qiushengming.business.yaozhi;

import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author qiushengming
 * @date 2018/10/13
 */
public class Monitored extends BaseWebCrawler{
  /**
   * 站点名称设置
   *
   * @return 站点名称
   */
  @Override
  protected String getSiteName() {
    return null;
  }

  /**
   * 初始化资源方法
   *
   * @return URL
   */
  @Override
  protected List<URL> initURL() {
    return null;
  }

  /**
   * 下载逻辑
   *
   * @param url {@link URL}
   */
  @Override
  protected Response download(URL url) {
    return null;
  }

  /**
   * 解析方法
   *
   * @param r {@link Response}
   * @return Boolean
   */
  @Override
  protected Boolean parser(Response r) {
    return null;
  }

  /**
   * 消息订阅
   *
   * @param responses 爬虫的启动时间
   */
  @Override
  protected void notice(List<Response> responses) throws IOException {

  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    return null;
  }
}
