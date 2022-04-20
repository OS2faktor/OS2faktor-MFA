package dk.digitalidentity.os2faktor.radius;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusException;
import org.tinyradius.util.RadiusServer;

import dk.digitalidentity.os2faktor.service.OS2faktorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Component
public class OS2faktorRadiusServer extends RadiusServer {

	@Autowired
	private OS2faktorService os2faktorService;

	@Value("${radius.sharedsecret}")
	private String sharedSecret;

	public OS2faktorRadiusServer() {
		this.executor =  Executors.newFixedThreadPool(5);
	}

	@Override
	public String getSharedSecret(InetSocketAddress client) {
		return sharedSecret;
	}

	@Override
	public RadiusPacket accountingRequestReceived(AccountingRequest accountingRequest, InetSocketAddress client) throws RadiusException {
		throw new UnsupportedOperationException("accounting is not supported!");
	}

	@Override
	public String getUserPassword(String userName) {
		throw new UnsupportedOperationException("getUserPassword should not be called during the login flow");
	}

	@Override
	public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client) throws RadiusException {
		String username = accessRequest.getUserName();
		String password = accessRequest.getUserPassword(); // requires PAP (does not support CHAP)

		log.info("accessRequestReceived for " + accessRequest.getUserName());
		int type = RadiusPacket.ACCESS_REJECT;
		if (os2faktorService.verifyPassword(username, password)) {
			type = RadiusPacket.ACCESS_ACCEPT;
		}
		
		RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
		copyProxyState(accessRequest, answer);

		return answer;
	}
}
