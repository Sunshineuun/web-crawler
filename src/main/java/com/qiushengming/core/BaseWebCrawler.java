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
import com.qiushengming.utils.DataToExecl;
import com.qiushengming.utils.DateUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public abstract class BaseWebCrawler {

  protected static final String TEMP_PATH = System.getProperty("java.io.tmpdir");

  protected Logger log = LoggerFactory.getLogger(getClass());

  protected Download download;

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
    if (download == null) {
      download = initDownload();
    }
    return download;
  }

  protected Download initDownload(){
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
  @Scheduled(cron = "0 0/1 * * * ?")
  public void start() {
    try {
      log.debug("启动 ------");
      if (isLock) {
        log.debug("已有线程进行中 ------");
        return;
      }

      log.debug("进行数据采集 ------");

      isLock = Boolean.TRUE;

      /*getUrlPool().getUrlPoolService().deleteByType(crawlerUuid());
      getResponseResultService().deleteByType(crawlerUuid());
      getConfigService().deleteById(crawlerUuid());*/

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

      _notice(responses);

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

  /**
   * map.putAll(null)，会报错
   */
  protected void initConfig() {
    crawlerConfig = configService.findConfigByCrawlerUUID(crawlerUuid);
    if (crawlerConfig == null || crawlerConfig.getConfig().isEmpty()) {
      crawlerConfig = new CrawlerConfig();
      crawlerConfig.setId(crawlerUuid);

      Map<String, Object> otherConfig = getCrawlerConfig();
      if (!CollectionUtils.isEmpty(otherConfig)) {
        crawlerConfig.putAll(otherConfig);
      }
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

  protected void putURL(URL url) {
    url.setType(crawlerUuid);
    getUrlPool().put(url);
  }

  /**
   * 再对象唯一判断，设置的字段名。在输出到Excel表格中，已表格的列头为准。
   * @return  String[]
   */
  protected String[] getIsExitKey() {
    return new String[]{"标题"};
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
    if (download != null) {
      download.quit();
      download = null;
    }
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
      putURL(url);
    }
    log.debug("共初始化资源：{}", urls.size());
  }

  private void _downloads() {
    URL url;
    long index = -1L;
    //StopWatch stopWatch = new StopWatch();
    while ((url = getUrlPool().get(crawlerUuid())) != null) {
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
    List<Response> list = responseResultService.findAllIsEnable(crawlerUuid());
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

  private void _notice(List<Response> responses) {
    List<Map<String, Object>> datas = notice(responses);

    // TODO 在这里拦截过滤已通知过的数据

    if(!datas.isEmpty()){

      //数据 to Excel
      SXSSFWorkbook wb = DataToExecl.createSXSSFWorkbook();
      Sheet sheet = DataToExecl.createSheet(wb);
      DataToExecl.writeData(Arrays.asList(getTitles()), datas, sheet);

      File file = new File(String.format("%s/%s_%s.xlsx", TEMP_PATH, getSiteName(), DateUtils
          .nowDate()));
      try {
        wb.write(new FileOutputStream(file));
      } catch (IOException e) {
        log.error("{}", e);
      }
      getEmailTool().sendSimpleMail(getSiteName(), file);
    }
  }

  /**
   * 通知
   *
   * @param e 异常信息
   */
  private void notice(Exception e) {
    emailTool.sendSimpleMail(e.toString(),"qiushengming@aliyun.com");
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
  protected abstract List<Map<String, Object>> notice(List<Response> responses);

  protected abstract Map<String, Object> getCrawlerConfig();

  protected abstract String[] getTitles();

  protected static String getAbsUrl(String absolutePath, String relativePath) {
    try {
      java.net.URL absoluteUrl = new java.net.URL(absolutePath);
      java.net.URL parseUrl = new java.net.URL(absoluteUrl, relativePath);
      return parseUrl.toString();
    } catch (MalformedURLException e) {
      return "";
    }
  }
}
