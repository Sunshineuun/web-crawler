package com.qiushengming.service.impl;

import com.qiushengming.entity.BaseEntity;
import com.qiushengming.service.ManagementService;
import com.qiushengming.utils.GenericsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.List;

public class AbstractManagementService<T extends BaseEntity>
    implements ManagementService<T> {

  Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  private MongoOperations mongoOperations;

  private Class entityClass;

  @Override
  public MongoOperations getMongoOperations() {
    return mongoOperations;
  }

  @Override
  public void save(T o) {
    getMongoOperations().save(o);
  }

  @Override
  public void deleteByType(String type) {
    getMongoOperations().remove(Query.query(Criteria.where("type").is(type)),getEntityClass());
  }

  @Override
  public List<T> findAllIsEnable() {
    Query query = Query.query(Criteria.where("isEnable").is(1));
    return getMongoOperations().find(query, getEntityClass());
  }

  @Override
  public void clear(String type) {
    Query query = Query.query(Criteria.where("isEnable").is(1).and("type").is(type));
    Update update = new Update();
    update.set("isEnable", 9);
    update.set("updateTime", new Date());
    getMongoOperations().updateMulti(query, update, getEntityClass());
  }

  @Override
  public Boolean isExist(T o) {
    T o1 = getMongoOperations().findById(o.getId(), getEntityClass());
    if (o1 == null || o1.getIsEnable() != 1) {
      return Boolean.FALSE;
    }
    return  Boolean.TRUE;
  }

  Class<T> getEntityClass() {
    if (entityClass == null) {
      entityClass =
          GenericsUtils.getSuperClassGenricType(this.getClass());
    }
    return entityClass;
  }

}
