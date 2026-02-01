package koscom.mini_project.team2.team2.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.remeik.site", description = "Production Server"),
                @Server(url = "http://localhost:8080", description = "Local Serever")
        }
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Koscom Team2 - REMEIK")
                        .version("1.0.0")
                        .description("ETF 추천 및 리밸런싱 서비스"));
    }
}
