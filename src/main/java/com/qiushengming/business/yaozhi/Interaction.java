package com.qiushengming.business.yaozhi;

import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DataToExecl;
import com.qiushengming.utils.DateUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 药智网药品相互作用 - https://db.yaozh.com/interaction/1.html
 */
@Service("Interaction")
public class Interaction extends BaseWebCrawler {

  private static final String URL_TEMPLATE = "https://db.yaozh.com/interaction/%s.html";

  @Override
  protected String getSiteName() {
    return "药智网-药品相互作用";
  }

  /**
   * 每月1日执行一次 cron = "0 0 0 1 * ? "
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 ? * 6")
  /*@Scheduled(cron = "0 0/1 * * * ? ")*/
  public void start() {
    super.start();
  }

  @Override
  protected List<URL> initURL() {
    List<URL> urls = new ArrayList<>();

    if (getUrlPool().get(crawlerUuid()) != null) {
      return urls;
    }

    Integer start = (Integer) crawlerConfig.get("start");
    Integer end = (Integer) crawlerConfig.get("end");

    for (Integer i = start; i <=100; i++) {
      URL url = new URL();
      url.setUrl(String.format(URL_TEMPLATE, i.toString()));
      urls.add(url);
    }
    return urls;
  }

  @Override
  protected Response download(URL url) {

    updateConfig(url);

    Response response = getDownload().get(url);

    parser(response);

    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      log.error("{}", e);
    }

    return response;
  }

  @Override
  protected Boolean parser(Response r) {
    if (!r.getDatas().isEmpty()) {
      return Boolean.TRUE;
    }
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Elements tables = doc.select("table.table");
      if (tables.size() < 2) {
        r.setHtml("");
        return Boolean.FALSE;
      }
      String drugName = tables.get(0).selectFirst("td").text().trim();

      for (int i = 1; i < tables.size(); i++) {
        Element e = tables.get(i);
        Elements tds = e.select("td");

        Map<String, Object> map = new HashMap<>();
        map.put("drug", drugName);
        map.put("drug_effect", tds.get(0).text());
        map.put("xg", tds.get(1).text());

        Data data = new Data();
        data.setResponseId(r.getId());
        data.setData(map);
        r.addData(data);
      }

      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}", e);
    }
    return Boolean.FALSE;
  }

  @Override
  protected void notice(List<Response> responses) throws IOException {
    List<Data> notices = getNoticeData(responses);

    //数据转换
    String[] titles = {"名称", "药品", "相互作用药品", "相互作用效果", "ID"};
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Data d : notices) {
      Map<String, Object> map = new HashMap<>();
      map.put("名称", getSiteName());
      map.put("药品", d.get("drug"));
      map.put("相互作用药品", d.get("drug_effect"));
      map.put("相互作用效果", d.get("xg"));
      map.put("ID", d.getId());
      datas.add(map);
    }

    if (!datas.isEmpty()) {
      //数据 to Excel
      SXSSFWorkbook wb = DataToExecl.createSXSSFWorkbook();
      Sheet sheet = DataToExecl.createSheet(wb);
      DataToExecl.writeData(Arrays.asList(titles), datas, sheet);

      File file = new File(String.format("%s/%s_%s.xlsx", TEMP_PATH, getSiteName(), DateUtils
          .nowDate()));
      wb.write(new FileOutputStream(file));
      getEmailTool().sendSimpleMail(getSiteName(), file);
    }
  }

  protected List<Data> getNoticeData(List<Response> responses) {
    List<Data> notices = new ArrayList<>();
    for (Response r : responses) {
      notices.addAll(r.getDatas());
    }
    return notices;
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    Map<String, Object> map = new HashMap<>();
    map.put("start", 1);
    map.put("end", 6928);
    return map;
  }

  /**
   * 这里交互会很频繁，是否需要每次都往数据库中提交呢
   *
   * @param url {@link URL}
   */
  private void updateConfig(URL url) {
    crawlerConfig.setUrl(url);
    getConfigService().updateConfig(crawlerConfig);
  }

  @Override
  protected boolean isClean() {
    return Boolean.FALSE;
  }
}
