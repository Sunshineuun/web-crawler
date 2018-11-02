package com.qiushengming.business.medlive;

import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DateUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * 医脉通咨询监测 - http://news.medlive.cn/all/info-progress/list.html?ver=branch
 "心血管":1,"神经内科":2,"消化科":3,"肝病":4,"内分泌":5,"肿瘤":6,"血液科":7,精神科":8,
 "呼吸科":9,""肾病": 10,"风湿免疫":11,"感染科":12,"普通外科":13,"神经外科":14,"胸心外科":15,
 "泌尿外科":16,"骨科":17,"整形外科":18,"麻醉科":19,"妇产科":20,"儿科":21,"眼科":22,
 "耳鼻咽喉科":23,"口腔科":24,""皮肤性病科":25,"急诊/重症":26,影像科":27,"检验科":28,
 *
 * 数据采集的过程中发现，div_type=all，并不能采集全咨询
 */
@Service("News")
public class News extends Medlive {

  private static final String URL_DOMAIL = "http://news.medlive.cn";

  @Override
  protected String getSiteName() {
    return "医脉通-咨询";
  }

  /**
   * 1. 断掉有两种情况如下： * 当前请求结束了 1）会根据当前的page+1，新增一个URL * 当前请求没有结束；进行到一半的时候，例如下载的内容并且保存了，但是URL未更新（这里也不影响，无非再下载一遍咯）
   * 2）直接使用当前的page
   *
   * @return {@link URL}
   */
  @Override
  protected List<URL> initURL() {
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

    // 决定偷偷懒了，一个月最多更新100页吧。
    for (Integer i = 1; i < 29; i++) {
      urls.add(getURL(getParmas(page + 1, i)));
    }
    urls.add(getURL(getParmas(page + 1, "all")));
    return urls;
  }

  protected URL getURL(Map<String, Object> params) {
    URL url = new URL();
    url.setUrl("http://news.medlive.cn/cms.php");
    // 组建参数
    url.setParams(params);

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json, text/javascript, */*");
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept-Encoding", "gzip, deflate");
    url.setHeaders(headers);

    return url;
  }

  private Map<String, Object> getParmas(int page, Object type) {
    Map<String, Object> map = new HashMap<>();
    map.put("submit_type", "ajax");
    map.put("ac", "research_branch");
    map.put("div_type", type);
    map.put("model_type", "info");
    map.put("cat_type", "research");
    map.put(getPageKey(), page);
    return map;
  }

  @Override
  protected Response download(URL url) {
    Integer page = (Integer) url.getParams().get(getPageKey());
    Object divType = url.getParams().get("div_type");
    log.info("page is:{} - {}", page, divType);

    updateConfig(url);

    Response response = getDownload().fromSubmit(url);

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
      putURL(getURL(getParmas(page + 1, divType)));
    }

    try {
      Thread.sleep(2500);
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
      Map map = GSON.fromJson(r.getHtml(), Map.class);
      String htmlKey = "html";
      // 需要验证(map.get(dataListKey) instanceof List)是否成立
      if (map != null && map.containsKey(htmlKey)) {
        Document doc = Jsoup.parse((String) map.get(htmlKey));
        Elements divs = doc.select("div.item_dotted_split");
        for (Element e : divs) {
          Element e1 = e.selectFirst("div.title");
          String title = e1.text();
          String url = URL_DOMAIL + e1.selectFirst("a").attr("href");
          // 描述
          String dsc = e.selectFirst("div.memo").text();
          // 时间
          String date = e.selectFirst("span.date_time").text();

          Map<String, Object> o = new HashMap<>();
          o.put("title", title);
          o.put("dsc", dsc);
          o.put("guide_url", url);
          o.put("publish_date", date);

          Data data = new Data();
          data.setResponseId(r.getId());
          data.setData(o);
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
    return new String[]{"*药*与*药*配伍禁忌*",};
  }
}
