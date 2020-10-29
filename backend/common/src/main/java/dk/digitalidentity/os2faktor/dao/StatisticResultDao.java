package dk.digitalidentity.os2faktor.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2faktor.dao.model.StatisticResult;

public interface StatisticResultDao extends JpaRepository<StatisticResult, Long> {
	List<StatisticResult> getAllByCvr(String cvr);
}
