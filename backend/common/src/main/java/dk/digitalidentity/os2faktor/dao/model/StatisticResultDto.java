package dk.digitalidentity.os2faktor.dao.model;

public interface StatisticResultDto {
	Long getLogins();
	Long getServerId();
	String getCvr();
	String getMunicipalityName();
	String getServerName();
}
