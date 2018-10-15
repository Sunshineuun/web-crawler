package com.qiushengming.business;

import static com.qiushengming.common.Symbol.BLANK;

import com.qiushengming.common.download.Download;
import com.qiushengming.common.download.HttpClinentDownload;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DateUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 药品安全合作联盟 - http://www.psmchina.cn/index <br> 1. 国内最新医药政策 - http://www.psmchina.cn/safe_medicines_trends/medical_policies/
 * <br> 2. 国内药品安全通报 - http://www.psmchina.cn/safe_medicines_trends/Chinese_Adverse_Drug_Reaction_Information_Bulletin/<br>
 * 3. 国外药物警戒快讯 - http://www.psmchina.cn/safe_medicines_trends/Pharmacovigilance_News/ <br> 4.
 * 国外用药安全时讯 - http://www.psmchina.cn/safe_medicines_trends/yyaqsx <br> 4.1 药物警戒 - /ywjj/ <br> 4.2
 * 高危药品 - /gwyp/ <br> 4.3 用药安全文化建设 - /yyaqwhjs/ <br> 4.4 静脉用药安全 - /jmyyaq/ <br> 4.5 时讯速递 - /sxsd/
 * <br> 4.6 举案论错 - /jalc/ <br> 4.7 用药安全课堂 - /yyaqkt/ <br> 4.8 药师与患者 - /ysyhz/ <br>
 *
 * 问题
 *  1. 关于HTTP 406问题排查
 *    参考地址 - http://mobile.51cto.com/hot-558405.htm
 * @author MinMin
 */
@Service("Psmchina")
public class Psmchina extends Medlive {

  private static final String URL_DOMAIN = "http://www.psmchina.cn";

  @Override
  protected String getSiteName() {
    return "药品安全合作联盟";
  }

  @Override
  protected Download getDownload() {
    return new HttpClinentDownload();
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

  /**
   * 1. 断掉有两种情况如下： * 当前请求结束了 1）会根据当前的page+1，新增一个URL * 当前请求没有结束；进行到一半的时候，例如下载的内容并且保存了，但是URL未更新（这里也不影响，无非再下载一遍咯）
   * 2）直接使用当前的page
   *
   * @return {@link URL}
   */
  @Override
  protected List<URL> initURL() {
    String[] urlstr = {
        "http://www.psmchina.cn/safe_medicines_trends/medical_policies/",
        "http://www.psmchina.cn/safe_medicines_trends/Chinese_Adverse_Drug_Reaction_Information_Bulletin/",
        "http://www.psmchina.cn/safe_medicines_trends/Pharmacovigilance_News/",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/ywjj",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/gwyp",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/yyaqwhjs",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/jmyyaq",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/sxsd",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/jalc",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/yyaqkt",
        "http://www.psmchina.cn/safe_medicines_trends/yyaqsx/ysyhz",
    };
    List<URL> urls = new ArrayList<>();

    int page = (int) crawlerConfig.get(getPageKey());
    if (page == -1) {
      page = 0;
    }

    if (crawlerConfig.getUrl() != null) {
      if (getUrlPool().isExist(crawlerConfig.getUrl())) {
        return urls;
      }
    }

    for (String s : urlstr) {
      urls.add(getURL(s, getParmas(page + 1)));
    }
    return urls;
  }

  @Override
  protected Response download(URL url) {
    int page = (int) url.getParams().get(getPageKey());
    log.info("page is:{}, URL is:{}",  page, url.getUrl());

    updateConfig(url);

    if (page == 1) {
      url.removeParamsKey(getPageKey());
    }

    Response response = getDownload().fromSubmit(url);

    if (page == 1) {
      url.putParamsKey(getPageKey(), 1);
    }

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
      putURL(getURL(url.getUrl(), getParmas(((Integer)url.getParams().get(getPageKey())) + 1)));
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
      Map resultMap = GSON.fromJson(r.getHtml(), Map.class);
      String html = (String)resultMap.get("data");
      Document doc = Jsoup.parse(html);
      if (StringUtils.equals("success", (String)resultMap.get("msg"))) {
        for (Element e : doc.select("dl")) {
          Element a = e.selectFirst("h4 > a");
          String title = a.attr("title");
          String url = URL_DOMAIN + a.attr("href");
          // 描述
          String dsc = e.selectFirst("p.p1 > a").attr("title");
          // 日期
          String publishDate = e.selectFirst("p.p2").text().replaceAll("[^0-9^\\-]", BLANK);

          Map<String, Object> map = new HashMap<>();
          map.put("title", title);
          map.put("guide_url", url);
          map.put("dsc", dsc);
          map.put("publish_date", publishDate);

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

  @Deprecated
  @Override
  protected URL getURL(Map<String, Object> params) {
    return null;
  }

  private URL getURL(String urlStr, Map<String, Object> params) {
    URL url = new URL();
    url.setUrl(urlStr);
    // 组建参数
    url.setParams(params);

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "*/*");
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put("Accept-Encoding", "gzip, deflate");
    url.setHeaders(headers);
    return url;
  }

  @Override
  protected Map<String, Object> getParmas(int page) {
    Map<String, Object> map = new HashMap<>();
    map.put("p", page);
    return map;
  }

  @Override
  protected String[] getKeys() {
    return new String[]{"*药品不良反应*", "*不良反应*", "*药品*风险*", "*药品*毒性*", "*使用*风险*",};
  }

  @Override
  protected String getPageKey() {
    return "p";
  }
}
