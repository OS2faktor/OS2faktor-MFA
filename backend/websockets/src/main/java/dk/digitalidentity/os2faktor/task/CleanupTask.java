package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.SocketHandler;

@Component
@EnableScheduling
public class CleanupTask {

	@Autowired
	private SocketHandler socketHandler;

	@Scheduled(fixedRate = 5 * 60 * 1000)
	private void cleanupSessions() {
		socketHandler.closeStaleSessions();
	}
}
