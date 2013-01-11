package com.skunk.clock.db;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Clocktime {
	private LinkedList<Entry<Long, Long>> times = new LinkedList<Entry<Long, Long>>();
	private int missingABadge = 0;

	Clocktime() {
	}

	public void clockIn(boolean hadBadge) {
		times.add(new SimpleEntry<Long, Long>(System.currentTimeMillis(),
				System.currentTimeMillis()));
		if (!hadBadge) {
			missingABadge++;
		}
	}

	public void clockOut(boolean hadBadge) {
		if (isClockedIn()) {
			times.getLast().setValue(System.currentTimeMillis());
			if (!hadBadge) {
				missingABadge++;
			}
		}
	}

	public int getMissingBadgeCount() {
		return missingABadge;
	}

	public void clockOutWith(int i) {
		if (isClockedIn()) {
			times.getLast().setValue(times.getLast().getKey() + i);
		}
	}

	public boolean isClockedIn() {
		return times.size() > 0
				&& times.getLast().getValue().equals(times.getLast().getKey());
	}

	public String getChunksString() {
		StringBuilder builder = new StringBuilder();
		builder.append(times.size());
		for (Entry<Long, Long> clock : times) {
			builder.append(',').append(clock.getKey()).append(',')
					.append(clock.getValue());
		}
		return builder.toString();
	}

	public long getClockTime() {
		long time = 0;
		for (Entry<Long, Long> clock : times) {
			time += clock.getValue() - clock.getKey();
		}
		return time;
	}
}
