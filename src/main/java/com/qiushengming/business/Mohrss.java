package com.qiushengming.business;

import com.qiushengming.common.Symbol;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 人力资源社会保障部各省市人社部 - http://www.mohrss.gov.cn/SYrlzyhshbzb/shehuibaozhang/zcwj/yiliao/index.html
 */
@Service("Mohrss")
public class Mohrss extends Medlive {

  private static final String URL_DOMAIN = "http://www.mohrss.gov.cn/SYrlzyhshbzb/shehuibaozhang/zcwj/yiliao";
  private static final String URL_TEMPLATE = URL_DOMAIN + "/index%s.html";

  @Override
  protected String getSiteName() {
    return "人力资源社会保障部各省市人社部";
  }

  /**
   * 每周六执行一次 cron = "0 0 0 0 0 6 "
   */
  @Async
  @Override
  @Scheduled(cron = "0 0 0 ? * 6")
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
    return url;
  }

  @Override
  protected Map<String, Object> getParmas(int page) {
    Map<String, Object> map = new HashMap<>();
    map.put(getPageKey(), page);
    return map;
  }

  @Override
  protected Boolean parser(Response r) {
    if (!r.getDatas().isEmpty()) {
      return Boolean.TRUE;
    }
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Element el = doc.selectFirst("div.organGeneralNewListConType");
      if (el == null) {
        return Boolean.FALSE;
      }

      for (Element e : el.getAllElements()) {
        Elements spans = e.select("span.organMenuTxtLink");
        String title = spans.get(0).text();
        String url = getAbsUrl(r.getUrl().getUrl(), spans.get(0).attr("href"));
        String date = spans.get(1).text();

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
    return new String[]{"*基本医疗保险*药品目录*",};
  }

}
