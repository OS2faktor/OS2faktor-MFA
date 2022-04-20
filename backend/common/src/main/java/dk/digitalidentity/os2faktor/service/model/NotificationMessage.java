package dk.digitalidentity.os2faktor.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class NotificationMessage {
	
	@JsonProperty("default")
	@Getter
	@Setter
	private String _default;
	
	@JsonProperty("APNS")
	@Getter
	private String apns;
	
	@JsonProperty("GCM")
	@Getter
	private String gcm;
	
	@JsonProperty("ADM")
	@Getter
	private String adm;

	@Getter
	@Setter
	@AllArgsConstructor
	private class GCM {
		private Data data;
	}
	
	@Getter
	@Setter
	@AllArgsConstructor
	private class ADM {
		private Data data;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	private class APNS {
		private APS aps;
	}
	
	@Getter
	@Setter
	@AllArgsConstructor
	private class Data {
		private String message;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	private class APS {
		private String alert;
		private String sound = "default";
		
		public APS(String alert) {
			this.alert = alert;
		}
	}

	public void setGCM(String gcm) {
		this.gcm = gcm;
	}

	public void setADM(String adm) {
		this.adm = adm;
	}

	public void setAPNS(String apns) {
		this.apns = apns;
	}

	public Data createData(String message) {
		return new Data(message);
	}

	public APS createAPS(String alert) {
		return new APS(alert);
	}

	public ADM createADM(Data data) {
		return new ADM(data);
	}
	
	public GCM createGCM(Data data) {
		return new GCM(data);
	}
	
	public APNS createAPNS(APS aps) {
		return new APNS(aps);
	}
}
