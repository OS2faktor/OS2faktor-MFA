package dk.digitalidentity.nemid.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = { "classpath:pid.properties" })
public class NemIDConfigurationReader {

}
