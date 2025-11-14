package est.DreamDecode.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dreamDecodeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DreamDecode API")
                        .description("DreamDecode 서비스의 REST API 문서입니다.")
                        .version("v1")
                        .license(new License().name("MIT License"))
                        .contact(new Contact()
                                .name("DreamDecode Team")
                                .email("support@dreamdecode.app")))
                .addServersItem(new Server().url("http://localhost:8080").description("로컬"))
                .components(new Components());
    }
}

