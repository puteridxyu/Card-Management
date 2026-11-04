package com.bank.document_management.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DateTimeUtil {

	public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

	private DateTimeUtil() {
	}

	public static OffsetDateTime now() {
		return OffsetDateTime.now(ZONE);
	}

	public static OffsetDateTime toOffset(LocalDateTime value) {
		return value == null ? null : value.atZone(ZONE).toOffsetDateTime();
	}
}
