package dk.digitalidentity.os2faktor.service.model;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2faktor.service.model.enums.MessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CustomWebSocketPayload<T extends Serializable> {
	private String id;
	private MessageType type;
	private T data;
	private boolean reply;

	public CustomWebSocketPayload(String id, MessageType type, T data, boolean reply) {
		this.id = id;
		this.type = type;
		this.data = data;
		this.reply = reply;
	}

	public String toJSONString() {
		ObjectMapper mapper = new ObjectMapper();
		String json = null;

		try {
			json = mapper.writeValueAsString(this);
		}
		catch (JsonProcessingException ex) {
			log.error("Failed to serialize object", ex);
		}
		
		return json;
	}
}
