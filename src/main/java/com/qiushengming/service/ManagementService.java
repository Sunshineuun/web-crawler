package com.qiushengming.service;

import com.qiushengming.entity.BaseEntity;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.List;

public interface ManagementService<T extends BaseEntity> {
  MongoOperations getMongoOperations();

  /**
   * 保存资源
   *
   * @param o entity
   */
  void save(T o);

  List<T> findAllIsEnable();

  /**
   * 将{@link BaseEntity#isEnable} 更新为 9；
   */
  void clear(String type);

  /**
   * 是否存在
   * @param o object
   * @return Boolean
   */
  Boolean isExist(T o);
}
