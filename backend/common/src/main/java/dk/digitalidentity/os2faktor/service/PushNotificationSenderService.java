package dk.digitalidentity.os2faktor.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.model.NotificationMessage;
import dk.digitalidentity.os2faktor.service.model.PushStatus;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.EndpointDisabledException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;

@Slf4j
@Service
public class PushNotificationSenderService {

	@Autowired
	private SnsClient snsClient;

	@Value("${aws.sns.applicationArnGCM}")
	private String applicationArnGCM;

	@Value("${aws.sns.applicationArnAPNS}")
	private String applicationArnAPNS;

	@Value("${aws.sns.applicationArnGCMChrome}")
	private String applicationArnGCMChrome;

	public PushStatus publish(String message, String endpointArn) {
		try {
			ObjectMapper mapper = new ObjectMapper();

			NotificationMessage nm = new NotificationMessage();			
			nm.set_default(message);
			nm.setADM(mapper.writeValueAsString(nm.createADM(nm.createData(message))));
			nm.setGCM(mapper.writeValueAsString(nm.createGCM(nm.createData(message))));
			nm.setAPNS(mapper.writeValueAsString(nm.createAPNS(nm.createAPS(message))));

			String msg = mapper.writeValueAsString(nm);

			PublishRequest publishRequest = PublishRequest.builder()
				.targetArn(endpointArn)
				.message(msg)
				.messageStructure("json")
				.build();

			snsClient.publish(publishRequest);
		}
		catch (Exception ex) {
			if (ex instanceof EndpointDisabledException) {
				return PushStatus.DISABLED;
			}

			log.error("Failed to generate push notification message for " + endpointArn, ex);
			return PushStatus.FAILURE;
		}
		
		return PushStatus.SUCCESS;
	}

	public String createEndpoint(String token, String uuid, ClientType clientType) {
		String endpointArn = null;

		try {
			String appArn;

			switch (clientType) {
				case ANDROID:
					appArn = applicationArnGCM;
					break;
				case IOS:
					appArn = applicationArnAPNS;
					break;
				case CHROME:
					appArn = applicationArnGCMChrome;
					break;
				default:
					log.error("Unsupported ClientType: " + clientType);
					return null;
			}

			CreatePlatformEndpointRequest cpeReq = CreatePlatformEndpointRequest.builder()
					.token(token)
					.customUserData(uuid)
					.platformApplicationArn(appArn)
					.build();

			CreatePlatformEndpointResponse cpeRes = snsClient.createPlatformEndpoint(cpeReq);
			endpointArn = cpeRes.endpointArn();

			Map<String, String> attribs = new HashMap<String, String>();
			attribs.put("Enabled", "true");

			SetEndpointAttributesRequest saeReq = SetEndpointAttributesRequest.builder()
					.endpointArn(endpointArn)
					.attributes(attribs)
					.build();

			snsClient.setEndpointAttributes(saeReq);
		}
		catch (Exception ex) {
			if (ex instanceof InvalidParameterException) {
				InvalidParameterException ipe = (InvalidParameterException) ex;

				String message = ipe.awsErrorDetails().errorMessage();
	
				Pattern p = Pattern.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same Token.*");
				Matcher m = p.matcher(message);
				if (m.matches()) {
					endpointArn = m.group(1);

					// Endpoint already exists. We re-enable it and update user data (userData = deviceId, status = active)
					// TODO: update endpointArn! at AWS - it might be disabled/deactivated!
					
					return endpointArn;
				}
				
				if (message.contains("iOS device tokens must be no more than 400")) {
					log.error("Bad iOS token '" + token + "'");
				}
			}

			log.error("Failed to generate endpointArn for " + clientType.toString() + " / " + uuid + " / " + token, ex);
			return null;
		}

		return endpointArn;
	}
}