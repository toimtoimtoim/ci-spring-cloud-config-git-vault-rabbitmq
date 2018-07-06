package ee.tehik.example.spring_cloud_config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RefreshScope
@RestController
public class ClientApplication {

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${secret.from.vault}")
  private String secret;

  @RequestMapping("/hello")
  public Map<String, String> hello() {
    return new HashMap<String, String>() {{
      put("spring.datasource.url", url);
      put("secret.from.vault", secret);
    }};
  }

  public static void main(String[] args) {
    SpringApplication.run(ClientApplication.class, args);
  }
}
