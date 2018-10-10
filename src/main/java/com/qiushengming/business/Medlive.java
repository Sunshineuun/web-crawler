package com.qiushengming.business;

import com.google.gson.Gson;
import com.qiushengming.common.download.Download;
import com.qiushengming.common.download.HttpClinentDownload;
import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DataToExecl;
import com.qiushengming.utils.DateUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 医脉通监测 - http://guide.medlive.cn/
 * 1. 数据请求的地址
 *  * http://guide.medlive.cn/ajax/load_more.ajax.php?branch=0&sort=publish&year=0&type=all&page=6
 *  * branch=0 ?
 *  * sort=publish 排序
 *  * year=0
 *  * type=all
 *  * page=6
 * 2. 数据请求是通过表单提交的方式进行查询的
 *  * 可以观察Headers中，Query String Parameters 和 Form Data 在后台获取是不一样的。
 * 3. 下载的内容进行解析
 *  * 解析存储的速度有点慢，并且数据更新错了，之前指定的Class错了
 */
@Service("Medlive")
public class Medlive extends BaseWebCrawler {
  protected static final String[] KEYS = {"*超说明书*", ".*超药物说明书*", "*超药品说明书*",};
  private static final Gson GSON = new Gson();
  private static final String URL_TEMPLATE = "http://guide.medlive.cn/ajax/load_more.ajax.php";

  @Override
  protected String getSiteName() {
    return "医脉通";
  }

  @Override
  protected Download getDownload() {
    return new HttpClinentDownload();
  }

  /**
   * 每月1日执行一次 cron = "0 0 0 1 * ? "
   *
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 1 * ? ")
  public void start(){
    super.start();
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    if(((int) crawlerConfig.get("page")) == -1){
      crawlerConfig.put("publish_date", crawlerConfig.get("start_date"));
      crawlerConfig.put("start_date", DateUtils.nowDate("yyyy-MM-dd"));
    }
  }

  /**
   * 1. 断掉有两种情况如下：
   *  * 当前请求结束了
   *    1）会根据当前的page+1，新增一个URL
   *  * 当前请求没有结束；进行到一半的时候，例如下载的内容并且保存了，但是URL未更新（这里也不影响，无非再下载一遍咯）
   *    2）直接使用当前的page
   * @return {@link URL}
   */
  @Override
  protected List<URL> initURL() {
    List<URL> urls = new ArrayList<>();

    int page = (int) crawlerConfig.get("page");
    if (page == -1) {
      page = 0;
    }

    if (crawlerConfig.getUrl() != null) {
      if (getUrlPool().isExist(crawlerConfig.getUrl())) {
        return urls;
      }
    }

    // 决定偷偷懒了，一个月最多更新100页吧。
    urls.add(getURL(getParmas(page+1)));
    return urls;
  }

  protected URL getURL(Map<String,Object> params) {
    URL url = new URL();
    url.setUrl(URL_TEMPLATE);
    // 组建参数
    url.setParams(params);
    return url;
  }

  protected Map<String,Object> getParmas(int page) {
    Map<String, Object> map = new HashMap<>();
    map.put("branch", 0);
    map.put("sort", "publish");
    map.put("year", 0);
    map.put("type", "all");
    map.put("page", page);
    return map;
  }

  @Override
  protected Response download(URL url) {
    log.info("page is:{}",  url.getParams().get("page"));

    updateConfig(url);

    Response response = getDownload().fromSubmit(url);

    // 接着判断获取文章的日期是否大于配置设置的日期
    parser(response);
    Boolean bool = Boolean.FALSE;
    for (Data data : response.getDatas()) {
      // 当前文章日期 > 设置时间
       bool = DateUtils.compare(String.valueOf(data.get("publish_date")),
          String.valueOf(crawlerConfig.get("publish_date")), "yyyy-MM-dd");
      if (!bool) {
        break;
      }
    }

    // 如果当前文章列表中的所有文章都日期都大于预设日期，那么将进行翻页操作
    if (bool) {
      getUrlPool().put(getURL(getParmas(((int)url.getParams().get("page")) + 1)));
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.error("{}", e);
    }

    return response;
  }

  /**
   * 这里交互会很频繁，是否需要每次都往数据库中提交呢
   * @param url {@link URL}
   */
  void updateConfig(URL url) {
    crawlerConfig.put("page", url.getParams().get("page"));
    crawlerConfig.setUrl(url);
    getConfigService().updateConfig(crawlerConfig);
  }

  @Override
  protected Boolean parser(Response r) {
    if(!r.getDatas().isEmpty()){
      return Boolean.TRUE;
    }
    try {
      Map map = GSON.fromJson(r.getHtml(), Map.class);
      String dataListKey = "data_list";
      // 需要验证(map.get(dataListKey) instanceof List)是否成立
      if (map != null && map.containsKey(dataListKey)
          && map.get(dataListKey) instanceof List) {
        for (Object o : (List) map.get(dataListKey)) {
          if (o instanceof Map) {
            Data data = new Data();
            data.setResponseId(r.getId());
            data.setData((Map<String, Object>) o);
            r.addData(data);
          }
        }
      }
      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}",e);
    }
    return Boolean.FALSE;
  }

  /**
   * 1. 判断关键内容是否包含{@link Medlive#KEYS} 中的关键字
   * 2. 不含跳过被检测的数据
   * 3. 包含，新增到通知历史中；并保留到通知列表中(notices.add())，并且记录包含的关键字。
   * 4. 将通知数据按照一定格式写入到Excel文件中
   * 5. 发送邮件通知
   * @param responses  当前爬虫有效结果
   */
  @Override
  protected void notice(List<Response> responses) throws IOException {
    List<Data> notices = getNoticeData(responses);

    //数据转换
    String[] titles = {"名称", "标题", "URL", "KEY", "ID"};
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Data d : notices) {
      Map<String, Object> map = new HashMap<>();
      map.put("名称", getSiteName());
      map.put("标题", d.get("title"));
      map.put("URL", d.get("guide_url"));
      map.put("KEY", d.getKey());
      map.put("ID", d.getId());
      datas.add(map);
    }

    //数据 to Excel
    SXSSFWorkbook wb = DataToExecl.createSXSSFWorkbook();
    Sheet sheet = DataToExecl.createSheet(wb);
    DataToExecl.writeData(Arrays.asList(titles), datas, sheet);

    File file = new File(String.format("%s/%s_%s.xlsx", TEMP_PATH, getSiteName(), DateUtils
        .nowDate()));
    wb.write(new FileOutputStream(file));
    getEmailTool().sendSimpleMail(getSiteName(), file);
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    /*
    * 为了配合本次应该抓取到哪里为止；
    * 规约已文章日期为准，抓取当前时间之后的文章，并会将最大的文章时间更新进来。
    * */
    Map<String, Object> map = new HashMap<>();
    map.put("publish_date", "2005-01-01");
    map.put("start_date", "2005-01-01");
    // 当page
    map.put("page", -1);
    return map;
  }

  @Override
  protected void afterOption() {
    crawlerConfig.put("page", -1);
    getConfigService().updateConfig(crawlerConfig);
  }

  @Override
  protected boolean isClean() {
    return ((Integer)crawlerConfig.get("page")) == -1;
  }

  protected List<Data> getNoticeData(List<Response> responses) {
    List<Data> notices = new ArrayList<>();
    for (Response r : responses) {
      for (Data d : r.getDatas()) {
        if (!StringUtils.isEmpty(d.get("title"))) {
          String title = String.valueOf(d.get("title"));
          for (String k : KEYS) {
            if (Pattern.matches(k.replaceAll("\\*", ".*"), title)) {
              d.setKey(k);
              notices.add(d);
              break;
            }
          }
        }
      }
    }
    return notices;
  }
}
