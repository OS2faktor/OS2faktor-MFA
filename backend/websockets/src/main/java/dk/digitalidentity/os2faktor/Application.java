package dk.digitalidentity.os2faktor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.aws.autoconfigure.cache.ElastiCacheAutoConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration;
import org.springframework.cloud.aws.autoconfigure.jdbc.AmazonRdsDatabaseAutoConfiguration;

@SpringBootApplication(scanBasePackages = "dk.digitalidentity")
@EnableAutoConfiguration(exclude = {
        ElastiCacheAutoConfiguration.class,
        ContextStackAutoConfiguration.class,
        AmazonRdsDatabaseAutoConfiguration.class
})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
