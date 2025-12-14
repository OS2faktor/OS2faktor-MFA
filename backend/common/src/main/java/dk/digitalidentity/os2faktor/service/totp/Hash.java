package dk.digitalidentity.os2faktor.service.totp;

public enum Hash {
	SHA1("HMACSHA1"),
	SHA256("HMACSHA256");

	private String hash;

	Hash(String hash) {
		this.hash = hash;
	}

	@Override
	public String toString() {
		return hash;
	}
}
