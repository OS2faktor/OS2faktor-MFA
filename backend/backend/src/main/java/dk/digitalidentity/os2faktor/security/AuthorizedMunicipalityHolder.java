package dk.digitalidentity.os2faktor.security;

import dk.digitalidentity.os2faktor.dao.model.Municipality;

public class AuthorizedMunicipalityHolder {
	private static final ThreadLocal<Municipality> municipalityHolder = new ThreadLocal<>();

	public static void setMunicipality(Municipality municipality) {
		municipalityHolder.set(municipality);
	}

	public static Municipality getMunicipality() {
		return municipalityHolder.get();
	}

	public static void clear() {
		municipalityHolder.remove();
	}
}
