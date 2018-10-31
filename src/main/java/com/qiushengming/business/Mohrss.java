package com.qiushengming.business;

import com.qiushengming.common.Symbol;
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
  /*@Scheduled(cron = "0 0/1 * * * ?")*/
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
      putURL(getURL(getParmas(((Integer) url.getParams().get(getPageKey())) + 1)));
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
      Elements els = doc.select("div.organGeneralNewListConType");
      if (els == null) {
        return Boolean.FALSE;
      }
      /*
      *   TODO
      *   编码有问题
      * */
      for (Element e : els){
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
