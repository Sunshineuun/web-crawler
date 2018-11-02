package com.qiushengming.business.yaozhi;

import static com.qiushengming.common.Symbol.BLANK;

import com.csvreader.CsvReader;
import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

/**
 * 药智网(超说明书药物) - https://db.yaozh.com/unlabeleduse?name=&csyz=&indication=
 *
 */
@Service
public class Unlabeleduse extends BaseWebCrawler {

  private static final String URL_TEMPLATE = "https://db.yaozh.com/unlabeleduse?name=%s&p=1&pageSize=30";

  @Override
  protected String getSiteName() {
    return "药智网-超说明书";
  }

  /**
   *
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 1 * ? ")
  /*@Scheduled(cron = "0 0/1 * * * ? ")*/
  public void start() {
    super.start();
  }

  @Override
  protected List<URL> initURL() {
    List<URL> urls = new ArrayList<>();

    // 只要好存在有效资源，则不初始化资源
    if (getUrlPool().get(crawlerUuid()) != null) {
      return urls;
    }

    File file;
    try {
      file = ResourceUtils.getFile("classpath:doc/药物成分表-大通用名.csv");
      CsvReader csvReader = new CsvReader(file.getPath(), ',', Charset.forName("utf-8"));
      String[] values;
      while (csvReader.readRecord()) {
        values = csvReader.getValues();
        URL url = new URL();
        url.setUrl(String.format(URL_TEMPLATE, values[0]));

        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        url.setParams(params);

        urls.add(url);
      }
    } catch (IOException e) {
      log.error("{}", e);
    }
    return urls;
  }

  @Override
  protected Response download(URL url) {
    Response response = getDownload().get(url);

    parser(response);

    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      log.error("{}", e);
    }

    return response;
  }

  /**
   * 1. 检测请求是否有效，无效将HTML置空
   * 2. 检测是否需要翻页，检测页码
   * @param r {@link Response}
   * @return Boolean
   */
  @Override
  protected Boolean parser(Response r) {
    if (!CollectionUtils.isEmpty(r.getDatas())) {
      return true;
    }
    if (StringUtils.isEmpty(r.getHtml())) {
      return false;
    }
    Document doc = Jsoup.parse(r.getHtml());
    Elements tbodys = doc.select("tbody");

    if (CollectionUtils.isEmpty(tbodys)) {
      // 获取到数据为空的时候返回false
      return false;
    }

    try {
      Elements trs = tbodys.get(0).select("tr");
      for (Element tr : trs) {
        Element a = tr.selectFirst("a");
        String url = getAbsUrl(r.getUrl().getUrl(), a.attr("href"));
        // 药物名称
        String drugName = tr.selectFirst("th").text();
        Elements td = tr.select("td");
        // 超说明书适应症
        String csms = td.get(0).text();
        // 批准适应症
        String pz = td.get(1).text();
        Map<String, Object> map = new HashMap<>();
        map.put("药物名称", drugName);
        map.put("超说明书适应症", csms);
        map.put("批准适应症", pz);
        map.put("URL", url);

        Data data = new Data();
        data.setResponseId(r.getId());
        data.setData(map);
        r.addData(data);
      }
      int page = (int)r.getUrl().getParams().get("page");
      if (page == 1) {
        // page
        Element span = doc.selectFirst("span.total-nums");
        int count = Integer.valueOf(span.text().replaceAll("[^0-9]", BLANK));
        int page_count = count / 30 + 1 > 7 ? 7 : count / 30 + 1;
        for (int i = 2; i <= page_count; i++) {
          URL url = new URL();
          url.setUrl(r.getUrl().getUrl().replace("p=1", "p=" + String.valueOf(i)));

          Map<String, Object> params = new HashMap<>();
          params.put("page", i);
          url.setParams(params);
          putURL(url);
        }
      }
      return true;
    } catch (Exception e) {
      log.error("{}", e);
    }
    return false;
  }

  @Override
  protected List<Map<String, Object>> notice(List<Response> responses) {
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Response r : responses) {
      for (Data d : r.getDatas()) {
        datas.add(d.getData());
      }
    }
    return datas;
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    return null;
  }

  @Override
  protected boolean isClean() {
    return false;
  }

  @Override
  protected String[] getTitles() {
    return new String[]{"药品名称","超说明书适应症","批准适应症", "URL","ID"};
  }
}
