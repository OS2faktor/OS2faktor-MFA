package dk.digitalidentity.os2faktor.radius;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RadiusWorker extends Thread {
	private OS2faktorRadiusServer radiusServer;
	private DatagramSocket s;
	private DatagramPacket packetIn;
	private InetSocketAddress localAddress;
	private InetSocketAddress remoteAddress;
	private RadiusPacket request;
	private String secret;

	// TODO: nope, not called anywhere
	public RadiusWorker(OS2faktorRadiusServer radiusServer, DatagramSocket s, DatagramPacket packetIn, InetSocketAddress localAddress, InetSocketAddress remoteAddress, RadiusPacket request, String secret) {
		this.radiusServer = radiusServer;
		this.s = s;
		this.packetIn = packetIn;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.request = request;
		this.secret = secret;
	}
/*
	@Override
	public void run() {
		try {
			RadiusPacket response = radiusServer.handlePacket(localAddress, remoteAddress, request, secret);
			
			// send response
			if (response != null) {
				log.info("send response: " + response);
	
				DatagramPacket packetOut = radiusServer.makeDatagramPacket(response, secret, remoteAddress.getAddress(), packetIn.getPort(), request);
				s.send(packetOut);
			}
			else {
				log.debug("no response sent");
			}
		}
		catch (SocketTimeoutException ste) {
			// this is expected behaviour
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
	*/
}
