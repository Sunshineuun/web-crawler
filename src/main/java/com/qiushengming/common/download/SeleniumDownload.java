package com.qiushengming.common.download;

import com.qiushengming.entity.Response;
import com.qiushengming.entity.URL;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 下载模块
 */
public class SeleniumDownload implements Download {

  /**
   * 驱动器的路径
   */
  private static final String CHROME_DRIVER_EXE_PROPERTY = "C:/chrome/chromedriver.exe";
  /**
   * Chrome运行日志存储的路径
   */
  private static final String CHROME_DRIVER_LOG_PROPERTY = "C:/chrome/chromedriver.log";

  private static ChromeDriverService driverService;

  private static Logger log = LoggerFactory.getLogger(SeleniumDownload.class);
  /**
   * 驱动 driver存在重复创建的可能性
   */
  private ChromeDriver driver;

  static {
    // 设置环境变量，chrome驱动的文件路径
    /*System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, CHROME_DRIVER_EXE_PROPERTY);*/
    // 设置环境变量，chrome驱动运行的日志文件存储地址
    /*System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, CHROME_DRIVER_LOG_PROPERTY);*/

    driverService = new ChromeDriverService.Builder()
        .usingDriverExecutable(new File(CHROME_DRIVER_EXE_PROPERTY)) // chrome驱动的文件路径
        .withLogFile(new File(CHROME_DRIVER_LOG_PROPERTY)) // chrome驱动运行日志存储位置
        .usingPort(8000) // 端口
        .build();
    try {
      driverService.start();
    } catch (IOException e) {
      log.error("DriverService{}", e);
    }
  }

  public SeleniumDownload() {
    this(Boolean.FALSE);
  }

  public SeleniumDownload(Boolean isProxy) {
    this.driver = createDriver(isProxy);
  }

  public WebDriver getDriver() {
    return this.driver;
  }

  /**
   * 1. 目前找不到获取响应码的途径，所以暂时不做校验
   *
   * @param url URL 必须已[http://]开头
   * @return pagesource
   */
  public Response get(URL url) {
    Response response = new Response(url);
    try {
      driver.get(url.getUrl());
      response.setHtml(driver.getPageSource());
    } catch (Exception e) {
      log.error("{}", e);
    }
    return response;
  }

  @Deprecated
  @Override
  public Response fromSubmit(URL url) {
    log.info("未来实现");
    return null;
  }

  @Deprecated
  @Override
  public Response fromData(URL url) {
    log.info("未来实现");
    return null;
  }

  public void quit() {
    if (driver != null) {
      driver.quit();
    }
  }

  public void updateProxy() {
    this.quit();
    this.driver = createDriver(Boolean.TRUE);
  }

  /**
   * @param isProxy 是否启用代理
   * @return ChromeDriver
   */
  private ChromeDriver createDriver(Boolean isProxy) {
    ChromeOptions options = createOptions(isProxy);

        /*ChromeDriverService.Builder builder = new ChromeDriverService.Builder();
        DriverService driverService = new ChromeDriverService(driverPath, 8000, );*/

    ChromeDriver driver = new ChromeDriver(driverService, options);

    // 进行隐式等待，等待1分钟
    driver.manage().timeouts()
        .implicitlyWait(1, TimeUnit.MINUTES)
        .pageLoadTimeout(1, TimeUnit.MINUTES)
        .setScriptTimeout(1, TimeUnit.MINUTES);

    return driver;
  }

  private ChromeOptions createOptions(Boolean isProxy) {
    ChromeOptions options = new ChromeOptions();
    // 代理设置,需要有一个代理池提供IP和端口 TODO
        /*if (isProxy && Boolean.FALSE) {
            options.addArguments("--proxy-server=http://202.20.16.82:10152");
        }*/

    // 禁用图片
    Map<String, Object> prefs = new LinkedHashMap<>();
    prefs.put("profile.managed_default_content_settings.images", 2);
    options.setExperimentalOption("prefs", prefs);

    return options;
  }
}
