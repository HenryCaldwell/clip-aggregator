package info.henrycaldwell.streamline.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "bearer-key";

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info().title("Streamline Admin API").version("2.0.0")
            .description("REST API for the Streamline admin console")
            .contact(new Contact().name("Henry Caldwell").email("henrycaldwell2005@gmail.com"))
            .license(new License().name("MIT").url("http://github.com/HenryCaldwell/streamline/blob/main/LICENSE")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
            new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
  }
}
