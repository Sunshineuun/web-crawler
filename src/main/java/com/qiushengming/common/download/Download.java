package com.qiushengming.common.download;

import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;

public interface Download {

  /**
   * 退出下载器
   */
  void quit();

  /**
   * GET 请求
   * 获取资源
   * @param url {@link URL}
   * @return 一般返回HTML
   */
  Response get(URL url);

  /**
   * POST请求
   * @param url {@link URL}
   * @return 一般返回HTML
   */
  Response fromSubmit(URL url);

  Response fromData(URL url);
}
