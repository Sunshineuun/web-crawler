package com.qiushengming.common;

import com.qiushengming.entity.BaseEntity;
import com.qiushengming.service.impl.AbstractManagementService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("EmailTool")
public class EmailTool
    extends AbstractManagementService<BaseEntity>
    implements InitializingBean {

  @Autowired
  private JavaMailSender sender;

  private final String collectionName = "web_cralwer_config";

  private Map<String, Object> configMap = new HashMap<>();

  public void sendSimpleMail(String content) {
    sendSimpleMail(content, new ArrayList<>());
  }

  public void sendSimpleMail(String content, File affix) {
    List<File> files = new ArrayList<>();
    files.add(affix);
    sendSimpleMail(content, files);
  }

  /**
   * 发送email
   *
   * @param content 正文内容
   * @param affix   附件
   */
  public void sendSimpleMail(String content, List<File> affix) {
    MimeMessage message;
    try {
      message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, Boolean.TRUE);
      helper.setFrom(getEmailFrom()); // 发件人
      helper.setTo(getEmailTo()); // 收件人
      helper.setSubject(getSubjcet());

      // 正文
      helper.setText(content, true);

      if (!CollectionUtils.isEmpty(affix)) {
        // 附件
        for (File f : affix) {
          FileSystemResource fileSystemResource = new FileSystemResource(f);
          helper.addAttachment(f.getName(), fileSystemResource);
        }
      }
      sender.send(message);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  /**
   * 更新配置信息 <br>
   * 1. 定时更新配置信息,每天0点，12点更新；
   */
  @Scheduled(cron = "0 0 0,12 * * ?")
  public void updateConfig() {
    configMap.putAll(getConfig());
  }

  /**
   * 收件人列表
   *
   * @return String[]
   */
  private String[] getEmailTo() {
    return (String[]) ((List)configMap.get("to")).toArray();
  }

  /**
   * 发件人
   *
   * @return String
   */
  private String getEmailFrom() {
    return (String) configMap.get("from");
  }

  private String getSubjcet() {
    return (String) configMap.get("subject");
  }

  /**
   * 获取配置信息
   *
   * @return 如果获取结果为null，则返回空的hashmap；否则返回查询结果
   */
  private Map<String, Object> getConfig() {
    Query query = Query.query(Criteria.where("key").is("email"));
    Map map1 = getMongoOperations().findOne(query, Map.class, collectionName);
    if (CollectionUtils.isEmpty(map1)) {
      return new HashMap<>();
    } else {
      return (Map<String, Object>) map1;
    }
  }

  /**
   * 初始化配置
   */
  private void initConfig() {
    configMap.putAll(getConfig());
  }

  @Override
  public void afterPropertiesSet() {
    // 从mongodb中初始化配置
    initConfig();
    // 判断配置是否存在
    if (CollectionUtils.isEmpty(configMap)) {
      // 如果不存在，则用代码中的默认配置进行初始化
      Map<String, Object> map = new HashMap<>();
      map.put("key", "email");
      String[] tos = {"qiushengming@aliyun.com"};
      map.put("to", tos);
      map.put("from", "qiushengming@tech-winning");
      map.put("subject", "爬虫通知");
      getMongoOperations().save(map, collectionName);
      configMap.putAll(map);
    }
  }
}
