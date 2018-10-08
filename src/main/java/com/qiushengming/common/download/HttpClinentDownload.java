package com.qiushengming.common.download;

import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;

public class HttpClinentDownload implements Download {

  private Logger log = LoggerFactory.getLogger(getClass());

  private CookieStore cookieStore = new BasicCookieStore();
  private CloseableHttpClient client = HttpClients.custom()
      .setDefaultCookieStore(cookieStore)
      .build();

  @Override
  public void quit() {
  }

  /**
   *
   * @param url {@link URL}
   * @return String
   */
  @Override
  public Response get(URL url) {
    Response responseResult = new Response(url);

    HttpPost post = getHttpPost(url);
    List<NameValuePair> list = getFromData(url);
    UrlEncodedFormEntity entity;
    try {
      if (!CollectionUtils.isEmpty(list)) {
        entity = new UrlEncodedFormEntity(list, "UTF-8");
        Assert.notNull(entity, "form data is not null");
        post.setEntity(entity);
      }

      HttpResponse response = getClient().execute(post);
      Assert.notNull(response, "response is not null");

      if (response.getStatusLine().getStatusCode() == SC_OK) {
        responseResult.setHtml(EntityUtils.toString(response.getEntity()));
      } else {
        // URL - 响应状态码
        log.info(String.format("%s - %s", url, response.getStatusLine().getStatusCode()));
      }

    }catch (ConnectTimeoutException e1){
      log.error("超时 - {}", e1);
    }catch (IOException e) {
      log.error("{}", e);
    }
    return responseResult;
  }

  /**
   * 创建post请求
   * @param url String
   * @return {@link HttpPost}
   */
  private HttpPost getHttpPost(URL url) {
    // 配置超时时间
    RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(getTimeout())
        .setConnectionRequestTimeout(getTimeout())
        .setSocketTimeout(getTimeout())
        .setRedirectsEnabled(true)
        .build();

    HttpPost post = new HttpPost(url.getUrl());
    post.setConfig(config);


    Header[] headers = getHeaders(url);
    post.setHeaders(headers);

    return post;
  }

  /**
   * 请求头设置
   * @return {@link Header}
   * @param url {@link URL}
   */
  private Header[] getHeaders(URL url) {
    List<Header> headers = new ArrayList<>();

    headers.add(new BasicHeader("User-Agent", getUserAgent()));
    headers.add(new BasicHeader("Connection", "keep-alive"));

    if (!CollectionUtils.isEmpty(url.getHeaders())) {
      for(String key: url.getHeaders().keySet()){
        headers.add(new BasicHeader(key, url.getHeaders().get(key)));
      }
    }

    Header[] h = new BasicHeader[headers.size()];
    return headers.toArray(h);
  }

  /**
   * 后续要将数据存储到数据库中随机获取
   * @return String User-Agent
   */
  private String getUserAgent() {
    return "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";
  }

  /**
   * 组装表单数据
   * @param url {@link URL}
   * @return {@link NameValuePair} list
   */
  private List<NameValuePair> getFromData(URL url) {
    if (CollectionUtils.isEmpty(url.getParams())) {
      return null;
    }
    Map<String, Object> params = url.getParams();
    List<NameValuePair> pairs = new ArrayList<>();
    for (String key : params.keySet()) {
      pairs.add(new BasicNameValuePair(key, String.valueOf(params.get(key))));
    }
    return pairs;
  }

  /**
   * 获取客户端
   * @return {@link HttpClient}
   */
  private HttpClient getClient() {
    return client;
  }

  /**
   * 超时时间
   * @return int
   */
  private int getTimeout() {
    return 1000;
  }
}
