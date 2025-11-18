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
                        .description("""
                                DreamDecode 서비스의 REST API 문서입니다.
                                
                                ## 주요 기능
                                - 사용자 인증 및 회원가입
                                - 꿈 등록, 조회, 수정, 삭제
                                - AI 기반 꿈 심리 분석
                                - 프로필 관리
                                
                                ## 인증
                                - 대부분의 API는 JWT 토큰 인증이 필요합니다.
                                - 로그인 후 발급받은 Access Token을 Authorization 헤더에 포함하거나 쿠키로 전송하세요.
                                """)
                        .version("v1")
                        .license(new License().name("MIT License"))
                        .contact(new Contact()
                                .name("DreamDecode Team")
                                .email("support@dreamdecode.app")))
                .addServersItem(new Server().url("http://localhost:8080").description("로컬"))
                .components(new Components());
    }
}

