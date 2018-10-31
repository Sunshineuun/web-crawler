package com.qiushengming.entity;

import com.qiushengming.common.Symbol;
import org.apache.http.HttpResponse;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 存储URL及其响应结果 <br>
 * 1. 响应结果，isEnable状态位说明 <br>
 * 1.1 需要不进行解析的isEnable = 2 <br>
 * 1.2 需要进行解析的isEnable = 1 <br>
 * 1.3 解析成功的isEnable = 0 <br>
 * 1.4 解析失败不做更新，会反复被解析，这里需要通知下，再议。 <br>
 */
@Document(collection = "Response")
public class Response
    extends BaseEntity
    implements Serializable {
  @Field("HTML")
  private String html = Symbol.BLANK;
  @Field("URL")
  private URL url;
  @Field("DATA")
  private List<Data> datas = new ArrayList<>();
  @Transient
  private transient HttpResponse httpResponse;

  public Response(URL url) {
    this.url = url;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public List<Data> getDatas() {
    return datas;
  }

  public void setDatas(List<Data> datas) {
    this.datas = datas;
  }

  public void addData(Data d) {
    getDatas().add(d);
  }

  public void addAllData(List<Data> ds) {
    getDatas().addAll(ds);
  }

  public HttpResponse getHttpResponse() {
    return httpResponse;
  }

  public void setHttpResponse(HttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }
}
