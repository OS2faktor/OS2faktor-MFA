package dk.digitalidentity.os2faktor.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.StatisticResultCurrent;

public interface StatisticResultCurrentDao extends JpaRepository<StatisticResultCurrent, Long> {
	StatisticResultCurrent getByCvr(String cvr);
}
