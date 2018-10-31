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

  /**
   * 删除全部
   */
  void deleteByType(String type);

  List<T> findAllIsEnable(String type);

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

  void deleteById(String s);
}
