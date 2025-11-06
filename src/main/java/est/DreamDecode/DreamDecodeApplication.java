package est.DreamDecode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan   // 👈 이거 추가!
public class DreamDecodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(DreamDecodeApplication.class, args);
	}

}
