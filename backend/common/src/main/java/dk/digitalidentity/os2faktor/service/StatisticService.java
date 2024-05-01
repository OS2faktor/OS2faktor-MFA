package dk.digitalidentity.os2faktor.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2faktor.dao.StatisticDao;
import dk.digitalidentity.os2faktor.dao.StatisticResultCurrentDao;
import dk.digitalidentity.os2faktor.dao.StatisticResultDao;
import dk.digitalidentity.os2faktor.dao.model.StatisticResult;
import dk.digitalidentity.os2faktor.dao.model.StatisticResultCurrent;
import dk.digitalidentity.os2faktor.dao.model.StatisticResultDto;
import dk.digitalidentity.os2faktor.dao.model.dto.StatisticResultDTO;

@Service
public class StatisticService {

	@Autowired
	private StatisticDao statisticDao;
	
	@Autowired
	private StatisticResultDao statisticResultDao;
	
	@Autowired
	private StatisticResultCurrentDao statisticResultCurrentDao;
	
	public List<StatisticResultDTO> getAllByCvr(String cvr) {
		SimpleDateFormat formatter = new SimpleDateFormat("MMM yyyy");
		List<StatisticResultDTO> result = new ArrayList<>();
		
		List<StatisticResult> statistics = statisticResultDao.findAllByCvr(cvr);
		for (StatisticResult sr : statistics) {
			StatisticResultDTO srDTO = new StatisticResultDTO();
			srDTO.setLogins(sr.getLogins());
			srDTO.setServerName(sr.getServerName());
			srDTO.setTts(formatter.format(sr.getTts()));
			
			result.add(srDTO);
		}
		
		return result;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void removeOldStatistics() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -90);
		Date timestamp = calendar.getTime();
		
		statisticDao.deleteByCreatedBefore(timestamp);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void generateDaily() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, cal.getMinimum(Calendar.DATE));

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String firstOfMonth = sdf.format(cal.getTime());
		
		List<StatisticResultDto> entries = statisticDao.generateStatisticsResult(firstOfMonth);
		
		for (StatisticResultDto entry : entries) {
			StatisticResultCurrent current = statisticResultCurrentDao.findByCvr(entry.getCvr());
			if (current == null) {
				current = new StatisticResultCurrent();
				current.setCvr(entry.getCvr());
				current.setMunicipalityName(entry.getMunicipalityName());
				current.setServerId(entry.getServerId());
				current.setServerName(entry.getServerName());
			}

			current.setLogins(entry.getLogins());
			
			statisticResultCurrentDao.save(current);
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void generateMonthly() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, cal.getMinimum(Calendar.DATE));

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String firstOfMonth = sdf.format(cal.getTime());
		
		Date today = new Date();

		List<StatisticResultDto> entries = statisticDao.generateStatisticsResult(firstOfMonth);
		for (StatisticResultDto entry : entries) {
			StatisticResult current = new StatisticResult();
			current.setCvr(entry.getCvr());
			current.setMunicipalityName(entry.getMunicipalityName());
			current.setServerId(entry.getServerId());
			current.setServerName(entry.getServerName());
			current.setLogins(entry.getLogins());
			current.setTts(today);
			
			statisticResultDao.save(current);
		}
	}
}
