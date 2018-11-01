package com.qiushengming.business;

import com.qiushengming.common.Symbol;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DateUtils;
import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ∈最新信息
 */
@Service
public class Nhfpc extends Medlive{
  private static final String URL_DOMAIN = "http://www.nhfpc.gov.cn/yaozs/pqt";
  private static final String URL_TEMPLATE = URL_DOMAIN + "/new_list%s.shtml";

  @Override
  protected String getSiteName() {
    return "中华人民共和国国家健康委员会";
  }

  /**
   * 每周六执行一次 cron = "0 0 0 0 0 6 "
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 1 * ? ")
  public void start() {
    super.start();
  }

  @Override
  protected URL getURL(Map<String, Object> params) {
    String page = Symbol.BLANK;
    if (((int) params.get(getPageKey())) > 1) {
      page = "_" + String.valueOf(params.get(getPageKey()));
    }

    URL url = new URL();
    url.setUrl(String.format(URL_TEMPLATE, page));
    // 组建参数
    url.setParams(params);

    Map<String, String> headers = new HashMap<>();
    url.setHeaders(headers);
    headers.put("Cookie", "banggoo.nuva.cookie=0|W9lcD|W9lW/;");
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Encoding", "gzip, deflate");
    headers.put("Accept-Language", "zh-CN,zh;q=0.9");
    return url;
  }

  private URL getURL(Map<String, Object> params, Response response) {
    String page = Symbol.BLANK;
    if (((int) params.get(getPageKey())) > 1) {
      page = "_" + String.valueOf(params.get(getPageKey()));
    }

    URL url = new URL();
    url.setUrl(String.format(URL_TEMPLATE, page));
    // 组建参数
    url.setParams(params);

    Map<String, String> headers = new HashMap<>();
    url.setHeaders(headers);
    HttpResponse httpResponse = response.getHttpResponse();
    String cookie = httpResponse.getHeaders("Set-Cookie")[0].getValue();
    headers.put("Cookie", cookie);
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Encoding", "gzip, deflate");
    headers.put("Accept-Language", "zh-CN,zh;q=0.9");
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
    log.info("page is:{}", url.getParams().get(getPageKey()));

    updateConfig(url);

    Response response = getDownload().get(url);

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
      putURL(getURL(getParmas(((Integer) url.getParams().get(getPageKey())) + 1), response));
    }

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
      Element el = doc.selectFirst("ul.zxxx_list");
      if (el == null) {
        return Boolean.FALSE;
      }

      for (Element e : el.select("li")) {
        Element a = e.child(0);
        String title = a.text();
        String url = getAbsUrl(r.getUrl().getUrl(), e.attr("href"));
        String date = e.child(1).text();

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("guide_url", url);
        map.put("publish_date", date);

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
  protected String[] getKeys() {
    return new String[]{"*新增*医疗*项目*","*国家基本药物*"};
  }
}
