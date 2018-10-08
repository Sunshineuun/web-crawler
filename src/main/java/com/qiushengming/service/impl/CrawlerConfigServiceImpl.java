package com.qiushengming.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.qiushengming.entity.CrawlerConfig;
import com.qiushengming.service.CrawlerConfigService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * 爬虫的配置信息
 */
@Service("CrawlerConfigService")
public class CrawlerConfigServiceImpl
    extends AbstractManagementService<CrawlerConfig>
    implements CrawlerConfigService {

  @Override
  public CrawlerConfig findConfigByCrawlerUUID(String crawlerUuid) {
    return getMongoOperations().findById(crawlerUuid, getEntityClass());
  }

  @Override
  public void updateConfig(CrawlerConfig c) {
    Query query = new Query(Criteria.where("id").is(c.getId()));
    Update update = Update.update("config", c.getConfig()).set("URL", c.getUrl());
    UpdateResult result = getMongoOperations().updateFirst(query, update, getEntityClass());
    log.debug("匹配到的数量：{}", result.getMatchedCount());
  }
}
