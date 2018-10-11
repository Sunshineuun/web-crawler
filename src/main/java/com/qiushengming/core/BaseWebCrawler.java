package com.qiushengming.core;

import com.qiushengming.common.EmailTool;
import com.qiushengming.common.URLPool;
import com.qiushengming.common.download.Download;
import com.qiushengming.common.download.SeleniumDownload;
import com.qiushengming.entity.CrawlerConfig;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.service.CrawlerConfigService;
import com.qiushengming.service.ResponseResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class BaseWebCrawler {

  protected static final String TEMP_PATH = System.getProperty("java.io.tmpdir");

  protected Logger log = LoggerFactory.getLogger(getClass());

  protected Download download = getDownload();

  protected CrawlerConfig crawlerConfig;

  protected Boolean isLock = Boolean.FALSE;

  @Resource(name = "URLPool")
  private URLPool urlPool;

  @Resource(name = "responseResultService")
  private ResponseResultService responseResultService;

  @Resource(name = "CrawlerConfigService")
  private CrawlerConfigService configService;

  @Resource(name = "EmailTool")
  private EmailTool emailTool;

  protected Download getDownload() {
    return new SeleniumDownload(Boolean.FALSE);
  }

  /**
   * 作为爬虫的唯一标识，可以是任何形式
   */
  private String crawlerUuid = crawlerUuid();

  /**
   * 站点名称
   */
  private String siteName = getSiteName();

  protected URLPool getUrlPool() {
    return urlPool;
  }

  protected ResponseResultService getResponseResultService() {
    return responseResultService;
  }

  protected CrawlerConfigService getConfigService() {
    return configService;
  }

  protected EmailTool getEmailTool() {
    return emailTool;
  }

  /**
   * 启动爬虫
   */
  public void start() {
    try {
      log.debug("启动 ------");
      if (isLock) {
        log.debug("已有线程进行中 ------");
        return;
      }
      log.debug("进行数据采集 ------");

      isLock = Boolean.TRUE;

      // 0. init
      initConfig();
      // 1. 持久化资源
      log.info("初始化URL");
      _initURL();
      // 2. 下载数据
      log.info("开始下载");
      _downloads();
      // 3. 解析数据
      log.info("开始解析");
      List<Response> responses = _parsers();
      // 4. 通知
      log.info("XSSF");

      notice(responses);

      // 5. 更新配置信息
      configService.updateConfig(crawlerConfig);
      // 6. 退出
      quit();

      afterOption();
    } catch (Exception e) {
      log.error("{}", e);
    }

    isLock = Boolean.FALSE;
  }

  /**
   * 整体流程结束之后的操作
   * 在线程解锁之前
   */
  protected void afterOption(){

  }

  protected void initConfig() {
    crawlerConfig = configService.findConfigByCrawlerUUID(crawlerUuid);
    if (crawlerConfig == null || crawlerConfig.getConfig().isEmpty()) {
      crawlerConfig = new CrawlerConfig();
      crawlerConfig.setId(crawlerUuid);
      crawlerConfig.putAll(getCrawlerConfig());
      configService.save(crawlerConfig);
    }

    if (isClean()) {
      // 清理数据
      clear();
    }
  }

  protected boolean isClean() {
    return Boolean.TRUE;
  }

  /**
   * 清理过往历史，实际是将过往
   */
  protected void clear() {
    log.info("清理数据....");
    getUrlPool().clear(crawlerUuid);
    getResponseResultService().clear(crawlerUuid);
    log.info("清理结束....");
  }

  /**
   * 设置爬虫的唯一标识
   *
   * @return String
   */
  protected String crawlerUuid() {
    return getClass().getSimpleName();
  }

  private void saveResponseReult(Response response) {
    response.setUpdateTime(new Date());
    response.setType(crawlerUuid);

    getResponseResultService().save(response);
    getUrlPool().updateSuccess(response.getUrl());
  }

  /**
   * 退出操作
   */
  private void quit() {
    getDownload().quit();
  }

  /**
   * 持久化资源 重复的资源不进行初始化，该怎么处理 TODO
   */
  private void _initURL() {
    List<URL> urls = initURL();

    /*if(!CollectionUtils.isEmpty(urls)){
      clear();
    }*/

    for (URL url : urls) {
      url.setType(crawlerUuid);
    }
    getUrlPool().put(urls);
    log.debug("共初始化资源：{}", urls.size());
  }

  private void _downloads() {
    URL url;
    long index = -1L;
    //StopWatch stopWatch = new StopWatch();
    while ((url = getUrlPool().get()) != null) {
      try {
        index++;
        if (index % 100 == 0) {
          log.debug("已请求的数量：{}", index);
        }
        //stopWatch.start();

        Response response = download(url);

        // 后期喜欢能返回Boolean值来决定当前请求是否有效
        if (!StringUtils.isEmpty(response.getHtml())) {
          response.setType(crawlerUuid);
          saveResponseReult(response);
        } else {
          // TODO
          // Python中是在这里调用的
          log.debug("URL:{}", url.toString());
        }

        //stopWatch.stop();
        //log.info("当前请求耗时" + stopWatch.getTotalTimeMillis());
      } catch (Exception e) {
        log.error("{}", e);
        // 出现异常需要进行通知
        notice(e);
      }
    }
  }

  /**
   * 1. 这个有个问题，就是findAll的话，会将所有数据取出来，占据内存比较大。 <br>
   */
  private List<Response> _parsers() {
    List<Response> list = responseResultService.findAllIsEnable();
    int index = 0;
    for (Response r : list) {
      index++;
      // 解析成功
      if (parser(r)) {
        //long s = System.currentTimeMillis();
        responseResultService.parserSuccess(r);
        //long e = System.currentTimeMillis();
        //log.info("耗时：{}", s-e);
        if (index % 100 == 0) {
          log.info("进度：{}/{}", index, list.size());
        }
      }
    }
    return list;
  }

  /**
   * 通知
   *
   * @param e 异常信息
   */
  private void notice(Exception e) {
    emailTool.sendSimpleMail(e.toString());
  }

  /**
   * 站点名称设置
   *
   * @return 站点名称
   */
  protected abstract String getSiteName();

  /**
   * 初始化资源方法
   *
   * @return URL
   */
  protected abstract List<URL> initURL();

  /**
   * 下载逻辑
   *
   * @param url {@link URL}
   */
  protected abstract Response download(URL url);

  /**
   * 解析方法
   *
   * @param r {@link Response}
   * @return Boolean
   */
  protected abstract Boolean parser(Response r);

  /**
   * 消息订阅
   *
   * @param responses 爬虫的启动时间
   */
  protected abstract void notice(List<Response> responses) throws IOException;

  protected abstract Map<String, Object> getCrawlerConfig();
}
