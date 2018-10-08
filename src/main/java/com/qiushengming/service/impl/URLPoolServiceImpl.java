package com.qiushengming.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.qiushengming.entity.URL;
import com.qiushengming.service.URLPoolService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

@Service("URLPoolService")
public class URLPoolServiceImpl
    extends AbstractManagementService<URL>
    implements URLPoolService {

  /**
   * 一条一条获取数据，需要测试性能。
   * 1. 为了让资源不反复被使用，给它增加使用上线，如果达到一定上限后，将不再被使用。
   *
   * 1. 当数据查询不到的情况会是怎么样的？
   *  * 查不到，返回的是null
   *
   * @return URL
   */
  @Override
  public URL getURL() {
    Query query = new Query(Criteria.where("isEnable").is(1));
    URL url = getMongoOperations().findOne(query, getEntityClass());

    if (url == null) {
      return null;
    }

    // 更新使用上线的次数
    Assert.notNull(url, "URL must not be null!");
    query = new Query(Criteria.where("id").is(url.getId()));
    Update update = new Update().set("uselimit", url.getUselimit() + 1);
    getMongoOperations().updateFirst(query, update, getEntityClass());
    return url;
  }

  @Override
  public void updateSuccess(URL url) {
    url.setUpdateTime(new Date());

    Query query = new Query(Criteria.where("id").is(url.getId()));
    Update update = Update.update("isEnable", 0);
    UpdateResult updateResult = getMongoOperations().updateFirst(query, update, URL.class);
    log.debug("更新结果的数量：{}", updateResult.getMatchedCount());
  }

}
