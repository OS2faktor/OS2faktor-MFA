package dk.digitalidentity.os2faktor.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "totph_devices")
public class HardwareToken {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@JsonIgnore
	@Column
	@NotNull
	private String serialnumber;

	@JsonIgnore
	@Column
	@NotNull
	private String secretKey;
	
	@Column
	@JsonIgnore
	@NotNull
	private boolean registered;

	@JsonIgnore
	@Column
	private String registeredToCpr;
	
	@JsonIgnore
	@Column
	private String registeredToCvr;
	
	@JsonIgnore
	@Column
	private String clientDeviceId;
	
	@JsonIgnore
	@Column
	private long offset;
}
