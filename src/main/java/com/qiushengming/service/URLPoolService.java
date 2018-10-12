package com.qiushengming.service;

import com.qiushengming.entity.URL;
import org.springframework.stereotype.Repository;

@Repository
public interface URLPoolService extends ManagementService<URL>{

  /**
   * 查询资源池， 建议增量获取，不要一次获取很多
   * @param type 类型
   * @return {@link URL}集合
   */
  URL getURL(String type);

  void updateSuccess(URL url);
}
