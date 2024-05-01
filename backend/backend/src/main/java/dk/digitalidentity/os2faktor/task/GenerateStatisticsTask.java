package dk.digitalidentity.os2faktor.task;

import java.util.Calendar;

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
public class GenerateStatisticsTask {
	
	@Autowired
	private StatisticService statisticService;
	
	@Value("${scheduled.run:false}")
	private boolean runScheduled;

	@Scheduled(cron = "0 50 23 * * ?")
	private void generateStatistics() {
		if (!runScheduled) {
			return;
		}

		log.info("Running daily statistics job");
		
		statisticService.generateDaily();

	    Calendar cal = Calendar.getInstance();
	    if (cal.get(Calendar.DATE) == cal.getActualMaximum(Calendar.DATE)) {
			log.info("Running monthly statistics job");
			
			statisticService.generateMonthly();
	    }
	    
		log.info("Done running daily statistics job");
	}
}
