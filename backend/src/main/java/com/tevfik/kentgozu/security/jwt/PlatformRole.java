package com.tevfik.kentgozu.security.jwt;

/**
 * KentGözü erişim sınıfları: vatandaş ve yönetici.
 */
public enum PlatformRole {

	CITIZEN("ROLE_CITIZEN"),
	ADMIN("ROLE_ADMIN");

	private final String authority;

	PlatformRole(String authority) {
		this.authority = authority;
	}

	public String authority() {
		return authority;
	}
}
