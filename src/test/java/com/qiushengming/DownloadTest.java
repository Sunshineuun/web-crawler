package com.qiushengming;

import com.qiushengming.common.download.Download;
import com.qiushengming.common.download.HttpClinentDownload;
import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadTest {
  public static void main(String[] args) {
    Download download = new HttpClinentDownload();
    URL url = new URL();
    url.setUrl("https://db.yaozh.com/monitored?p=4&pageSize=30");

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("cookie",
      "PHPSESSID=l918gg3ovkfjgl7f5fo5en0sn1; _ga=GA1.2.1765581520.1534991401; MEIQIA_EXTRA_TRACK_ID=19B6ye8ozmKTTPsG99Bj5xkrGwr; UtzD_f52b_auth=fb114aOver%2BWotIGfHB6clYfvFoQvgBjDqjAcx5m3Dx1Adm2J1AmexSE6So5wjuvzrzlvsdIaPG5pWTRro%2BR7v3JBYo; _ga=GA1.3.1765581520.1534991401; think_language=zh-CN; Hm_lvt_65968db3ac154c3089d7f9a4cbb98c94=1539405281; _gat=1; MEIQIA_VISIT_ID=1BVPR3063b2KHcYWmcOWzdgLPX2; ad_download=1; kztoken=nJail6zJp6iXaJqWl2xrZGFtZJaX; his=a%3A2%3A%7Bi%3A0%3Bs%3A28%3A%22nJail6zJp6iXaJqWl2xrZGFtY5uS%22%3Bi%3A1%3Bs%3A28%3A%22nJail6zJp6iXaJqWl2xrZGFtZJaX%22%3B%7D; Hm_lpvt_65968db3ac154c3089d7f9a4cbb98c94=1539405336");
    url.setHeaders(headers);

    Response r = download.get(url);
    System.out.println(1);
  }

  public static void a(List<String> a) {
    for (String s : a) {
      System.out.println(a);
    }
  }
}
