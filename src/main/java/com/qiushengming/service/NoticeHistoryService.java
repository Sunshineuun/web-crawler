package com.qiushengming.service;

import com.qiushengming.entity.NoticeHistory;
import org.springframework.stereotype.Repository;

/**
 * 存储通知历史的
 * 1. 为了过滤，已经通知过的内容
 */
@Repository
public interface NoticeHistoryService
    extends ManagementService<NoticeHistory>{
}
