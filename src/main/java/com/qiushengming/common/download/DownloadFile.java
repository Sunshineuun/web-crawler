package com.qiushengming.common.download;

import com.qiushengming.entity.URL;
import org.apache.http.HttpResponse;

public interface DownloadFile extends Download {
  HttpResponse downloadFile(URL url);
}
