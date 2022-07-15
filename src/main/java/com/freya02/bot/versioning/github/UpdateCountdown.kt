package com.freya02.bot.versioning.github;

import java.util.concurrent.TimeUnit;

public class UpdateCountdown {
	private final long interval;

	private long lastUpdate = 0;

	public UpdateCountdown(long time, TimeUnit unit) {
		this.interval = unit.toMillis(time);
	}

	public boolean needsUpdate() {
		if ((System.currentTimeMillis() - lastUpdate) > interval) {
			lastUpdate = System.currentTimeMillis();

			return true;
		} else {
			return false;
		}
	}
}
