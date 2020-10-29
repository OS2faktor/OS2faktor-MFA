package dk.digitalidentity.os2faktor.radius;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RadiusLauncher {

	@Autowired
	private OS2faktorRadiusServer radiusServer;
	
	@PostConstruct
	public void init() {
		radiusServer.start(true, false);
	}
}
