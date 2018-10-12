package com.qiushengming.business;

import static com.qiushengming.common.Symbol.BLANK;

import com.qiushengming.common.Symbol;
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
import org.apache.commons.lang3.StringUtils;
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
 * 国家药品不良反应监测中心 - http://www.cdr-adr.org.cn/xxtb_255/ypblfyxxtb/ 1.数据监测地址 * 1. 数据监测地址 *
 * http://www.cdr-adr.org.cn/xxtb_255/ypblfyxxtb/index%s.html 2. CDAADRypblfyxxtb 含义 - 药品不良反应信息通报
 */
@Service("CDAADRypblfyxxtb")
public class CDAADRypblfyxxtb extends Medlive {

  private static final String URL_DOMAIN = "http://www.cdr-adr.org.cn/xxtb_255/ypblfyxxtb";
  private static final String URL_TEMPLATE = "http://www.cdr-adr.org.cn/xxtb_255/ypblfyxxtb/index%s.html";

  @Override
  protected String getSiteName() {
    return "国家药品不良反应监测中心";
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
      putURL(getURL(getParmas(((int) url.getParams().get(getPageKey())) + 1)));
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
      Element el = doc.selectFirst("ul.list");
      if (el == null) {
        return Boolean.FALSE;
      }

      Map<String, Object> map = new HashMap<>();
      for (Element e : el.getAllElements()) {
        switch (e.tagName()) {
          case "li":
            Element a = e.selectFirst("a[href]");
            map.put("title", a.text());

            String url = a.attr("href");
            if (StringUtils.isEmpty(url)) {
              url = "";
            }
            map.put("guide_url", URL_DOMAIN + url.replaceAll("\\.", BLANK));
            break;
          case "span":
            Element span = e.selectFirst("span");
            map.put("publish_date", span.text().replaceAll("[\\[\\]]", BLANK));
            break;
          case "br":
            Data data = new Data();
            data.setData(map);
            data.setResponseId(r.getId());
            r.addData(data);
            map = new HashMap<>();
            break;
          default:
            break;
        }
      }
      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}", e);
    }
    return Boolean.FALSE;
  }

  protected String[] getKeys() {
    return new String[]{"*药品不良反应*", "*不良反应*", "*药品*风险*", "*药品*毒性*", "*使用*风险*",};
  }
}
