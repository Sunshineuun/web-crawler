package com.qiushengming.business.yaoliwang;

import static com.qiushengming.common.Symbol.BLANK;

import com.qiushengming.business.yaozhi.Interaction;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
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

@Service("Medicine")
public class Medicine extends Interaction {

  private static final String URL_DOMAIN = "http://www.yaoliwang.com";
  private static final String URL_TEMPLATE = "http://www.yaoliwang.com/medicine/search"
      + ".html?keyword=otcjialei&page=%s";

  @Async
  @Override
  @Scheduled(cron = "0 0 0 1 * ? ")
  public void start() {
    super.start();
  }

  @Override
  protected String getSiteName() {
    return "药历网-药品";
  }

  @Override
  protected List<URL> initURL() {
    List<URL> urls = new ArrayList<>();

    if (getUrlPool().get(crawlerUuid()) != null) {
      return urls;
    }

    Integer start = (Integer) crawlerConfig.get("start");
    Integer end = (Integer) crawlerConfig.get("end");

    for (Integer i = start; i <= end; i++) {
      URL url = new URL();
      url.setUrl(String.format(URL_TEMPLATE, i.toString()));
      urls.add(url);
    }
    return urls;
  }

  @Override
  protected Boolean parser(Response r) {
    if (!r.getDatas().isEmpty()) {
      return Boolean.TRUE;
    }
    if(StringUtils.isEmpty(r.getHtml())){
      return Boolean.FALSE;
    }
    switch (r.getUrl().getTree()) {
      case 0:
        return parser0(r);
      case 1:
        return parser1(r);
    }
    return Boolean.FALSE;
  }

  private Boolean parser0(Response r) {
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Element tbody = doc.selectFirst("tbody");
      if (tbody == null) {
        r.setHtml("");
        return Boolean.FALSE;
      }

      for (Element tr : tbody.select("tr")) {
        Element td = tr.selectFirst("td.lineheight18");
        if (td == null) {
          continue;
        }
        String url = URL_DOMAIN + td.child(0).attr("href");
        URL urlObj = new URL();
        urlObj.setUrl(url);
        urlObj.setTree(r.getUrl().getTree() + 1);
        putURL(urlObj);

        String drugname = td.child(0).child(0).text();

        Map<String, Object> map = new HashMap<>();
        map.put("drugname", drugname);
        map.put("url", url);

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

  private Boolean parser1(Response r) {
    String s1 = " 【是否处方】";
    String s2 = "\n 【通用名】";
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Element div = doc.selectFirst("div.userwidth2");
      Map<String, Object> map = new HashMap<>();
      for (int i = 0; i < div.childNodeSize(); i++) {
        String s = div.childNode(i).outerHtml();
        if(StringUtils.startsWith(s,s1)){
          map.put("是否处方", StringUtils.replaceAll(s, s1, BLANK));
        }
        if (StringUtils.startsWith(s,s2)) {
          map.put("通用名", StringUtils.replaceAll(s, s2, BLANK));
        }
      }

      map.put("TREE", r.getUrl().getTree());
      map.put("URL", r.getUrl().getUrl());

      Data data = new Data();
      data.setResponseId(r.getId());
      data.setData(map);
      r.addData(data);
      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}", e);
    }
    return Boolean.FALSE;
  }

  @Override
  protected List<Map<String, Object>> notice(List<Response> responses){
    List<Data> notices = getNoticeData(responses);

    //数据转换
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Data d : notices) {
      Map<String, Object> map = new HashMap<>();
      map.put("名称", getSiteName());
      map.put("通用名", d.get("通用名"));
      map.put("是否处方", d.get("是否处方"));
      map.put("ID", d.getId());
      datas.add(map);
    }

    return datas;
  }

  @Override
  protected List<Data> getNoticeData(List<Response> responses) {
    List<Data> notices = new ArrayList<>();
    for (Response r : responses) {
      if (r.getUrl().getTree() == 1) {
        notices.addAll(r.getDatas());
      }
    }
    return notices;
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    Map<String, Object> map = new HashMap<>();
    map.put("start", 1);
    map.put("end", 5927);
    return map;
  }

  @Override
  protected String[] getTitles() {
    return new String[]{"名称", "通用名", "是否处方", "ID"};
  }
}
