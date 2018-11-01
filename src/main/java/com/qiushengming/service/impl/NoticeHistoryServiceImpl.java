package com.qiushengming.service.impl;

import com.qiushengming.entity.NoticeHistory;
import com.qiushengming.service.NoticeHistoryService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service("NoticeHistoryImpl")
public class NoticeHistoryServiceImpl
    extends AbstractManagementService<NoticeHistory>
    implements NoticeHistoryService {
  @Override
  public Boolean isExist(NoticeHistory o) {
    Query query = Query.query(Criteria.where("key").is(o.getKey()));
    List<NoticeHistory> o1 = getMongoOperations().find(query,getEntityClass());
    if (CollectionUtils.isEmpty(o1)) {
      return Boolean.FALSE;
    }
    return  Boolean.TRUE;
  }
}
