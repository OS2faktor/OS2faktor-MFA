package dk.digitalidentity.os2faktor.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2faktor.service.StatisticService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class RemoveOldStatisticsTask {
	
	@Autowired
	private StatisticService statisticService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(cron = "0 0 19 * * SAT")
	private void cleanupStatistics() {
		if (!runScheduled) {
			return;
		}

		log.info("Running statistics cleanup job");
		
		statisticService.removeOldStatistics();
	}
}
