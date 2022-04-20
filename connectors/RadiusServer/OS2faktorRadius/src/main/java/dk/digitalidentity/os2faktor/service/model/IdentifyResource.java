package dk.digitalidentity.os2faktor.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentifyResource {

	@JsonProperty("urn:scim:schemas:extension:safewhere:identify:1.0")
	private IdentifyRecord record;
}
