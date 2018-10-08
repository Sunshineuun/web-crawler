package com.qiushengming.service;

import com.qiushengming.entity.Response;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseResultService
    extends ManagementService<Response> {

  /**
   * 解析成功，更新状态码（isEnable)
   * @param r 响应结果
   */
  void parserSuccess(Response r);
}
