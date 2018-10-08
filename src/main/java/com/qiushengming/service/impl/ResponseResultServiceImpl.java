package com.qiushengming.service.impl;

import com.mongodb.client.result.UpdateResult;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.service.ResponseResultService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * 响应结果存储
 */
@Service("responseResultService")
public class ResponseResultServiceImpl
    extends AbstractManagementService<Response>
    implements ResponseResultService {
  @Override
  public void parserSuccess(Response r) {
    Query query = new Query(Criteria.where("id").is(r.getId()));
    Update update = Update.update("isEnable", 0);
    UpdateResult updateResult = getMongoOperations().updateFirst(query, update, getEntityClass());
    log.debug("更新结果的数量：{}", updateResult.getMatchedCount());

    saveData(r);
  }

  private void saveData(Response r) {
    for (Data d : r.getDatas()) {
      d.setType(r.getType());
      getMongoOperations().save(d);
    }
  }
}
