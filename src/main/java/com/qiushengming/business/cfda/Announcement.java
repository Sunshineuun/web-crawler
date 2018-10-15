package com.qiushengming.business.cfda;

import static com.qiushengming.common.Symbol.BLANK;

import com.qiushengming.business.Medlive;
import com.qiushengming.common.Symbol;
import com.qiushengming.common.download.Download;
import com.qiushengming.common.download.SeleniumDownload;
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

@Service("Announcement")
public class Announcement extends Medlive {

  private static final String URL_DOMAIN = "http://samr.cfda.gov.cn/WS01/CL0007";
  private static final String URL_TEMPLATE = URL_DOMAIN + "/index%s.html";

  @Override
  protected String getSiteName() {
    return "国家食品药品监督管理总局-公告通知";
  }

  @Override
  protected Download getDownload() {
    if (download == null) {
      download = new SeleniumDownload(Boolean.FALSE);
    }
    return download;
  }

  /**
   * 每周六执行一次 cron = "0 0 0 0 0 6 "
   */
  @Async
  @Override
  @Scheduled(cron = "0 1/1 * * * ?")
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
    headers.put("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Cookie",
        "td_cookie=4208803451; FSSBBIl1UgzbN7N80S=toTE.T0ywDe03wCazGVINPa_9TQl9Pbvi4Vsb26NMhTSXMYPd4uTs9sX2lG6pq.D; FSSBBIl1UgzbN7N80T=22Wp5EDKAQM5wVrbe2A3Nt2KM.QI874D5wzMJZJ9kjpcxvakLZ5FUNw2_cPXNGbr._385EtLW_JZqbj9w85HLn9xC25gBAm1vu_T7JYzw6ELwIsr6dEWfVti2M3g90tzVxzh93cBbM4guuFTFe3kRq0WloRiUOgbNuhbPkI1aSu2AtBLjDoebseAvPfiAKA73bZ5KWFb_yITDWs_PlSXuA_4W8xfEsbPqarXrRdwpj7LPApl8lWXpkRpHJJj4xVBxZrIi29kEqgqfS56YUQXQOVCM12O.PHxWy2gIhCbuP3C__ULMGL4Iv9JB628Wm_pHSxFEtSW76.skMvb3vP0ug8dQycNV_gE0haJjS6NDxEieZskmicRutmVFjdCPPYbqTzc1At_VbDXh5KyEecZ.231d");
    url.setHeaders(headers);

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
      Elements trs = doc.selectFirst("td.2016_erji_content").select("tr");
      for (Element e : trs) {
        if (e.attributes().size() > 0 && StringUtils.equals(e.attr("class"), "")) {
          String date = e.select("span.listtddate15").text().replaceAll("[^0-9^\\-]", BLANK);
          String url = URL_DOMAIN + e.selectFirst("a").attr("href").replace("..", BLANK);
          String title = e.selectFirst("font").text();

          Map<String, Object> map = new HashMap<>();
          map.put("title", title);
          map.put("guide_url", url);
          map.put("publish_date", date);

          Data data = new Data();
          data.setResponseId(r.getId());
          data.setData(map);
          r.addData(data);
        }
      }
      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}", e);
    }
    return Boolean.FALSE;
  }

  @Override
  protected String[] getKeys() {
    return new String[]{"*药品不良反应*", "*不良反应*", "*药品*风险*",
        "*药品*毒性*", "*使用*风险*", "*儿童*禁用*", "*儿童*风险*",
        "*注射剂*临床*管理*", "*修订*说明书*", "*通过*仿制药*一致性*公告*",
    "*处方药*目录*", "*关于*处方药*通知*"};
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    /*
     * 为了配合本次应该抓取到哪里为止；
     * 规约已文章日期为准，抓取当前时间之后的文章，并会将最大的文章时间更新进来。
     * */
    Map<String, Object> map = new HashMap<>();
    map.put("publish_date", "1990-01-01");
    map.put("start_date", "1990-01-01");
    // 当page
    map.put(getPageKey(), -1);
    return map;
  }
}
