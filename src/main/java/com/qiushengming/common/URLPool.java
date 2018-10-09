package com.qiushengming.common;

import com.qiushengming.entity.URL;
import com.qiushengming.service.URLPoolService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 资源池模块/目前采用单例模式
 * 1. 这里纠结的是资源池模块，是单例呢？还是多例，给每个爬虫配置一个的 。
 * 2. 数据扭转
 * 2.1 put到mongodb中
 * 2.2 从mongodb中get出
 */
@Service("URLPool")
public class URLPool {

  @Resource(name = "URLPoolService")
  private URLPoolService urlPoolService;

  /**
   * 队列
   */
  private Queue<URL> urlQueue = new ArrayBlockingQueue<>(1000);

  public URLPool() {
    init();
  }

  /**
   * 获取资源
   *
   * @return {@link URL}
   */
  public URL get() {
    return urlPoolService.getURL();
  }

  /**
   * 单条增加
   *
   * @param url {@link URL}
   */
  public void put(URL url) {
    if (isExist(url)) {
      return;
    }
    urlPoolService.save(url);
  }

  /**
   * 批量增加
   *
   * @param urls {@link URL} 列表
   */
  public void put(List<URL> urls) {
    for (URL url : urls) {
      put(url);
    }
  }

  /**
   * 将当前URL更新为成功
   *
   * @param url {@link URL}
   */
  public void updateSuccess(URL url) {
    urlPoolService.updateSuccess(url);
  }

  private void init() {

  }

  public void clear(String type) {
    urlPoolService.clear(type);
  }

  public Boolean isExist(URL url) {
    return urlPoolService.isExist(url); // 存在
  }
}
