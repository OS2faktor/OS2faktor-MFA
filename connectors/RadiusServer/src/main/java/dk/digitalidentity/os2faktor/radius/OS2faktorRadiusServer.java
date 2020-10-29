package dk.digitalidentity.os2faktor.radius;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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

		int type = RadiusPacket.ACCESS_REJECT;
		if (os2faktorService.verifyPassword(username, password)) {
			type = RadiusPacket.ACCESS_ACCEPT;
		}
		
		RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
		copyProxyState(accessRequest, answer);

		return answer;
	}

	// This is a copy from version 1.0.1 of TinyRadius, modified so it spawns each request into it's on handler thread,
	// so the long-running OS2faktor validation does not block other requests from being processed
	@Override
	protected void listen(DatagramSocket s) {
		DatagramPacket packetIn = new DatagramPacket(new byte[RadiusPacket.MAX_PACKET_LENGTH], RadiusPacket.MAX_PACKET_LENGTH);

		while (true) {
			try {
				// receive packet
				try {
					s.receive(packetIn);

					if (log.isDebugEnabled()) {
						log.debug("receive buffer size = " + s.getReceiveBufferSize());
					}
				}
				catch (SocketException se) {
					if (se.getMessage().equalsIgnoreCase("Socket Closed")) {
						try {
							stop();
						}
						catch (Exception ex) {
							; // ignore
						}

						return;
					}

					log.error("SocketException during s.receive() -> retry", se);
					continue;
				}

				// check client
				InetSocketAddress localAddress = (InetSocketAddress)s.getLocalSocketAddress();
				InetSocketAddress remoteAddress = new InetSocketAddress(packetIn.getAddress(), packetIn.getPort());				
				String secret = getSharedSecret(remoteAddress);
				if (secret == null) {
					log.warn("ignoring packet from unknown client " + remoteAddress + " received on local address " + localAddress);
					
					continue;
				}
				
				// parse packet
				RadiusPacket request = makeRadiusPacket(packetIn, secret);
				log.info("received packet from " + remoteAddress + " on local address " + localAddress);

				// spawn thread, so we do not block
				new RadiusWorker(this, s, packetIn, localAddress, remoteAddress, request, secret).start();
			}
			catch (SocketTimeoutException ste) {
				// this is expected behavior
				log.trace("normal socket timeout");
			}
			catch (IOException ioe) {
				// error while reading/writing socket
				log.error("communication error", ioe);
			}
			catch (RadiusException re) {
				// malformed packet
				log.error("malformed Radius packet", re);
			}
		}
	}
	
	// direct copy of TinyRadius 1.0.1, put into this class to ensure it is visible to RadiusWorker class
	@Override
	protected RadiusPacket handlePacket(InetSocketAddress localAddress, InetSocketAddress remoteAddress, RadiusPacket request, String sharedSecret) throws RadiusException, IOException {
		RadiusPacket response = null;
		
		// check for duplicates
		if (!isPacketDuplicate(request, remoteAddress)) {
			if (localAddress.getPort() == getAuthPort()) {

				// handle packets on auth port
				if (request instanceof AccessRequest) {
					response = accessRequestReceived((AccessRequest)request, remoteAddress);
				}
				else {
					log.error("unknown Radius packet type: " + request.getPacketType());
				}
			}
			else if (localAddress.getPort() == getAcctPort()) {

				// handle packets on acct port
				if (request instanceof AccountingRequest) {
					response = accountingRequestReceived((AccountingRequest)request, remoteAddress);
				}
				else {
					log.error("unknown Radius packet type: " + request.getPacketType());
				}
			}
			else {
				// ignore packet on unknown port
			}
		}
		else {
			log.debug("ignore duplicate packet");
		}

		return response;
	}

	// direct copy of TinyRadius 1.0.1, put into this class to ensure it is visible to RadiusWorker class
	protected DatagramPacket makeDatagramPacket(RadiusPacket packet, String secret, InetAddress address, int port, RadiusPacket request) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		packet.encodeResponsePacket(bos, secret, request);
		byte[] data = bos.toByteArray();
	
		return new DatagramPacket(data, data.length, address, port);
	}
}
