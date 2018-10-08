package com.qiushengming.common.download;

import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;

public interface Download {

  /**
   * 退出下载器
   */
  void quit();

  /**
   * 获取资源
   * @param url String
   * @return 一般返回HTML
   */
  Response get(URL url);
}
