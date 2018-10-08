package com.qiushengming;

import com.qiushengming.business.Medlive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class WebCrawlerApplicationTests {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Resource(name = "Medlive")
  private Medlive medlive;

  @Test
  public void contextLoads() {
    medlive.start();
  }

}
