package dk.digitalidentity.os2faktor.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.os2faktor.dao.model.Statistic;
import dk.digitalidentity.os2faktor.dao.model.StatisticResultDto;

public interface StatisticDao extends JpaRepository<Statistic, Long> {
	List<Statistic> findByCvr(String cvr);
	void deleteByCreatedBefore(Date timestamp);
	
	@Query(nativeQuery = true, value = "SELECT COUNT(*) AS logins, s.server_id AS serverId, s.cvr AS cvr, m.name AS municipalityName, serv.name AS serverName" + 
			"  FROM statistics s" + 
			"  JOIN municipalities m ON m.cvr = s.cvr" + 
			"  JOIN servers serv ON serv.id = s.server_id" + 
			"  WHERE created > ?1" + 
			"  GROUP by cvr, serverId, municipalityName, serverName")
	List<StatisticResultDto> generateStatisticsResult(String from);
}
