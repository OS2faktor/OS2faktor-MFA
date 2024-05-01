package dk.digitalidentity.os2faktor.dao.model.enums;

import lombok.Getter;

@Getter
public enum NSISLevel {
	NONE,
	LOW,
	SUBSTANTIAL,
	HIGH;
	
	public String toClaimValue() {
		switch (this) {
			case HIGH:
				return "High";
			case LOW:
				return "Low";
			case SUBSTANTIAL:
				return "Substantial";
			case NONE:
				return "Ingen";
		}
		
		return null;
	}
}
