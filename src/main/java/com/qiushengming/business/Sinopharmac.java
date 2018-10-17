package com.qiushengming.business;

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
 * 广东省药学会 - http://www.sinopharmacy.com.cn/
 * 1. 数据监测地址
 * * http://www.sinopharmacy.com.cn/download/p/1
 */
@Service("Sinopharmac")
public class Sinopharmac extends Medlive {
  private static final String URL_TEMPLATE = "http://www.sinopharmacy.com.cn/download/p/%s";
  private static final String URL_DOMAIL = "http://www.sinopharmacy.com.cn";

  @Override
  protected String getSiteName() {
    return "广东省药学会";
  }

  /**
   * 每月1日执行一次 cron = "0 0 0 1 * ? "
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 1 * ? ")
  public void start() {
    super.start();
  }

  @Override
  protected URL getURL(Map<String, Object> params) {
    URL url = new URL();
    url.setUrl(String.format(URL_TEMPLATE, params.get(getPageKey())));
    // 组建参数
    url.setParams(params);
    return url;
  }

  @Override
  protected Map<String, Object> getParmas(int page) {
    Map<String, Object> map = new HashMap<>();
    map.put(getPageKey(), page);
    return map;
  }

  @Override
  protected Response download(URL url) {
    int page = (int) url.getParams().get(getPageKey());

    log.debug("page is:{}", page);
    updateConfig(url);
    Response response = getDownload().get(url);

    // 解析数据
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
    // 这个小于6的操作，以后需要改改 TODO
    if (bool && page < 6) {
      getUrlPool().put(getURL(getParmas((page + 1))));
    }

    try {
      Thread.sleep(1000);
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
      Element el = doc.selectFirst("div.page1");
      Elements lis = el.select("li");
      for (Element li : lis) {
        Elements a = li.select("a[href]");
        String time = li.selectFirst("span.list-time").text()
            .replace("上传日期：","");
        String keyword = li.selectFirst("span.list-keyword").text();

        Map<String, Object> map = new HashMap<>();
        map.put("publish_date", time);
        map.put("list-keyword", keyword);
        map.put("html_url", URL_DOMAIL + a.get(0).attr("href"));
        map.put("title", a.get(0).text());
        map.put("doc_url", URL_DOMAIL + a.get(1).attr("href"));

        Data data = new Data();
        data.setData(map);
        data.setResponseId(r.getId());
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
    String[] titles = {"名称", "标题", "URL", "KEY", "ID"};
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Data d : notices) {
      Map<String, Object> map = new HashMap<>();
      map.put("名称", getSiteName());
      map.put("标题", d.get("title"));
      map.put("URL", d.get("html_url"));
      map.put("KEY", d.getKey());
      map.put("ID", d.getId());
      datas.add(map);
    }

    if(!datas.isEmpty()) {
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

  @Override
  protected String[] getKeys() {
    return new String[]{"*超说明书*", "*超药物说明书*", "*超药品说明书*", "*药物*专家共识*"};
  }
}
