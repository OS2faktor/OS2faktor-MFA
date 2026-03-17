package dk.digitalidentity.os2faktor.service.totp;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class IncrementalClock extends Clock {
    private final int interval;
	private int offset = 0;
	
	public IncrementalClock(int offset) {
		super();
		
		this.interval = 30;
		this.offset = offset;
	}

	@Override
	public long getCurrentInterval() {
		Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, offset);

        long currentTimeSeconds = calendar.getTimeInMillis() / 1000;

        return currentTimeSeconds / interval;
	}
	
	public int getOffset() {
		return offset;
	}
}
