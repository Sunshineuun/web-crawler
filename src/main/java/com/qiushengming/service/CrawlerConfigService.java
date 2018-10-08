package com.qiushengming.service;

import com.qiushengming.entity.CrawlerConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerConfigService
    extends ManagementService<CrawlerConfig> {
  CrawlerConfig findConfigByCrawlerUUID(String crawlerUuid);

  void updateConfig(CrawlerConfig crawlerConfig);
}
