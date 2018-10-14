package com.qiushengming.business.yaozhi;

import com.qiushengming.core.BaseWebCrawler;
import com.qiushengming.entity.Data;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import com.qiushengming.utils.DataToExecl;
import com.qiushengming.utils.DateUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 辅助与重点监控用药 - https://db.yaozh.com/monitored?p=4&pageSize=30
 * 1. URL 模板 https://db.yaozh.com/monitored?p=2&pageSize=20&time=2018
 * @author qiushengming
 * @date 2018/10/13
 */
@Service("Monitored")
public class Monitored extends BaseWebCrawler{

  private final String URL_TEMPLATE = "https://db.yaozh.com/monitored?p=%s&pageSize=30&time=%s";
  private final String[] TITLE_KEY = {"药物名称","药物剂型","地域或机构","发文时间","监管级别","政策文件"};

  /**
   * 站点名称设置
   *
   * @return 站点名称
   */
  @Override
  protected String getSiteName() {
    return "药智网-辅助与重点监控用药";
  }

  @Override
  @Scheduled(cron = "0 0/1 * * * ? ")
  public void start() {
    super.start();
  }

  /**
   * 初始化资源方法
   *
   * @return URL
   */
  @Override
  protected List<URL> initURL() {
    String[][] k = getURLParams();
    List<URL> urls = new ArrayList<>();

    for (String[] k1 : k) {
      int page = Integer.valueOf(k1[1]);

      for (int i = 1; i <= page / 30 + 1; i++) {
        URL url = new URL();
        url.setUrl(String.format(URL_TEMPLATE, i, k1[0]));
        urls.add(url);
      }
    }
    return urls;
  }

  /**
   * 下载逻辑
   *
   * @param url {@link URL}
   */
  @Override
  protected Response download(URL url) {
    Response r = getDownload().get(url);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.error("{}", e);
    }
    return r;
  }

  /**
   * 解析方法
   *
   * @param r {@link Response}
   * @return Boolean
   */
  @Override
  protected Boolean parser(Response r) {
    if(!r.getDatas().isEmpty()){
      return Boolean.TRUE;
    }
    try {
      Document doc = Jsoup.parse(r.getHtml());
      Element tbody = doc.selectFirst("tbody");
      if (tbody != null) {
        Elements trs = tbody.select("tr");
        for (Element tr : trs) {
          Map<String, Object> map = new HashMap<>();
          Elements tds = tr.getAllElements();
          for (int i = 0; i < TITLE_KEY.length; i++) {
            Element e = tds.get(i);
            map.put(TITLE_KEY[i], e.text());

            if (i == 0) {
              Element a = e.selectFirst("a");
              map.put("html_url", a.attr("href"));
            }
          }

          Data data = new Data();
          data.setData(map);
          data.setResponseId(r.getId());
        }
      }
      return Boolean.TRUE;
    } catch (Exception e) {
      log.error("{}",e);
    }
    return Boolean.FALSE;
  }

  /**
   * 消息订阅
   *
   * @param responses 爬虫的启动时间
   */
  @Override
  protected void notice(List<Response> responses) throws IOException {
    List<Map<String, Object>> datas = new ArrayList<>();
    for (Response r : responses) {
      for (Data d : r.getDatas()) {
        datas.add(d.getData());
      }
    }

    //数据 to Excel
    SXSSFWorkbook wb = DataToExecl.createSXSSFWorkbook();
    Sheet sheet = DataToExecl.createSheet(wb);
    DataToExecl.writeData(Arrays.asList(TITLE_KEY), datas, sheet);

    File file = new File(String.format("%s/%s_%s.xlsx", TEMP_PATH, getSiteName(), DateUtils
        .nowDate()));
    wb.write(new FileOutputStream(file));

    log.info("文件存储路径：{}", file.getAbsolutePath());
  }

  @Override
  protected Map<String, Object> getCrawlerConfig() {
    return null;
  }

  private String[][] getURLParams(){
    return new String[][] {
      {"2015-09-14","42",},
      {"2015-09-28","244"},
      {"2015-10","120"},
      {"2015-11","110"},
      {"2016-01","84"},
      {"2016-02","50"},
      {"2016-06","186"},
      {"2016-07-05","175"},
      {"2016-07-14","98"},
      {"2016-08","100"},
      {"2016-09","46"},
      {"2016-10","120"},
      {"2016-12-28","80"},
      {"2016-12-29","788"},
      {"2017-01","88"},
      {"2017-02","80"},
      {"2017-03","144"},
      {"2017-05","80"},
      {"2017-07","100"},
      {"2017-08","82"},
      {"2017-09-04","80"},
      {"2017-09-1","64"},
      {"2017-09-2","92"},
      {"2017-10","124"},
      {"2017-11-01","64"},
      {"2017-11-1","200"},
      {"2017-11-2","50"},
      {"2017-12-0","76"},
      {"2017-12-1","80"},
      {"2017-12-2","346"},
      {"2018-01-2","3734"},
      {"2018-02","140"},
      {"2018-03-0","94"},
      {"2018-03-1","50"},
      {"2018-03-2","60"},
      {"2018-04","146"},
      {"2018-06","28"},
      {"2018-08-0","120"},
      {"2018-08-1","144"},
      {"2018-08-2","137"}
    };
  }
}
