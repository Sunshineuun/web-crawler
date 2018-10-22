package com.qiushengming.business;

import static com.qiushengming.common.Symbol.BLANK;

import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DateUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 中国非处方药协会 - http://www.cnma.org.cn/home
 *
 */
@Service("Cnma")
public class Cnma extends Medlive{

  private static final String URL_DOMAIN = "http://www.cnma.org.cn";
  private static final String URL_TEMPLATE = URL_DOMAIN + "/Home/List/index/id/17/p/%s.html";

  @Override
  protected URL getURL(Map<String, Object> params) {
    URL url = new URL();
    url.setUrl(String.format(URL_TEMPLATE, params.get("page")));
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
  protected Response download(URL url) {
    Integer page = (Integer) url.getParams().get(getPageKey());
    log.info("page is:{} - {}", page);

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
      putURL(getURL(getParmas(page + 1)));
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

    if (StringUtils.isEmpty(r.getHtml())) {
      return Boolean.FALSE;
    }
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Element div = doc.selectFirst("div.Zxdt_bottom");
      Elements lis = div.select("li");
      for (Element li : lis) {
        Element a = li.child(0);
        Element span = li.child(1);

        String title = a.text();
        String url = URL_DOMAIN + a.attr("href");
        String date = span.text().replaceAll("[{}]", BLANK);

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("guide_url", url);
        map.put("publish_date", date);

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
  protected String[] getKeys() {
    return new String[]{"*处方药*目录*","*关于*处方药*通知*",};
  }
}
