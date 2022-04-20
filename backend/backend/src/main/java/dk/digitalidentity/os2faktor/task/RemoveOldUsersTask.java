package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class RemoveOldUsersTask {
	
	@Autowired
	private UserService userService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(cron = "${cron.removeOldUsers:0 0 20 * * SAT}")
	private void removeUsers() {
		if (!runScheduled) {
			return;
		}

		log.info("Running deleteAncientUsers");
		
		userService.deleteAncientUsers();
	}
}
