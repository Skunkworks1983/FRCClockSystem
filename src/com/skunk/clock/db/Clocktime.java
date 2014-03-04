package com.skunk.clock.db;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Clocktime implements Iterable<Entry<Long, Long>> {
	/**
	 * The list of check in/out pairs. If the clock in/out times are equal they
	 * haven't clocked out.
	 */
	private LinkedList<Entry<Long, Long>> times = new LinkedList<Entry<Long, Long>>();
	/**
	 * The number of check-ins/outs without a badge.
	 */
	private int checksWithoutBadges = 0;

	/**
	 * This object should only be created by the clocktime database.
	 */
	public Clocktime() {
	}

	/**
	 * Clocks this object in at the current time, increasing the missing badge
	 * count by one if they didn't have a badge.
	 * 
	 * @param hadBadge
	 *            if the check in was with a badge.
	 */
	public void clockIn(boolean hadBadge) {
		times.add(new SimpleEntry<Long, Long>(System.currentTimeMillis(),
				System.currentTimeMillis()));
		if (!hadBadge) {
			checksWithoutBadges++;
		}
	}

	/**
	 * Clocks this object in at the given time.
	 */
	public void adminClockIn(long time) {
		if (isClockedIn()) {
			times.set(times.size() - 1, new SimpleEntry<Long, Long>(time, time));
		} else {
			times.add(new SimpleEntry<Long, Long>(time, time));
		}
	}

	/**
	 * Clocks this object out at the given time.
	 */
	public void adminClockOut(long time) {
		if (isClockedIn()) {
			times.getLast().setValue(time);
		}
	}

	/**
	 * Clocks this object out at the current time, increasing the missing badge
	 * count by one if they didn't have a badge.
	 * 
	 * @param hadBadge
	 *            if the check out was with a badge.
	 */
	public void clockOut(boolean hadBadge) {
		if (isClockedIn()) {
			times.getLast().setValue(System.currentTimeMillis());
			if (!hadBadge) {
				checksWithoutBadges++;
			}
		}
	}

	/**
	 * Gets the number of times this object was clocked in or out without a
	 * badge.
	 * 
	 * @return the number of missing badges
	 */
	public int getMissingBadgeCount() {
		return checksWithoutBadges;
	}

	/**
	 * If this object is in the clocked-in state, clocks them out with the given
	 * number of milliseconds as their total time in the last data block.
	 * 
	 * @param time
	 *            the time in milliseconds
	 */
	public void clockOutWith(long time) {
		if (isClockedIn()) {
			times.getLast().setValue(times.getLast().getKey() + time);
		}
	}

	/**
	 * Checks if this user is clocked in.
	 * 
	 * @return the clocked-in state of the user
	 */
	public boolean isClockedIn() {
		return times.size() > 0
				&& times.getLast().getValue().equals(times.getLast().getKey());
	}

	/**
	 * Compiles this object as a string with the format
	 * "chunk count,start,end,start,end...", with the start and end being in
	 * unix-epoch time.
	 * 
	 * @return the compiled string.
	 */

	public String getChunksString() {
		StringBuilder builder = new StringBuilder();
		builder.append(times.size());
		for (Entry<Long, Long> clock : times) {
			builder.append(',').append(clock.getKey()).append(',')
					.append(clock.getValue());
		}
		return builder.toString();
	}

	/**
	 * Gets the total clock time, in milliseconds of this object.
	 * 
	 * @return the total clock time.
	 */
	public long getClockTime() {
		long time = 0;
		for (Entry<Long, Long> clock : times) {
			time += clock.getValue() - clock.getKey();
		}
		return time;
	}

	/**
	 * Sets the number of missing badges for this clock time database. Only
	 * called by the clocktime database class when loading cached data.
	 * 
	 * @param missing
	 *            the missing badge count
	 */
	void setMissingBadges(int missing) {
		this.checksWithoutBadges = missing;
	}

	/**
	 * Inserts a single clock time chunk into this clock time object, with the
	 * given start and end times, in unix-epoch form.
	 * 
	 * @param start
	 *            the start time
	 * @param end
	 *            the end time
	 */
	public void insertClockTime(long start, long end) {
		times.add(new SimpleEntry<Long, Long>(start, end));
	}

	public void insertClockTime(Clocktime db) {
		for (Entry<Long, Long> tt : db.times) {
			times.add(tt);
		}
	}

	@Override
	public Iterator<Entry<Long, Long>> iterator() {
		return times.iterator();
	}

	public Collection<? extends Entry<Long, Long>> getChunks() {
		// TODO Auto-generated method stub
		return null;
	}
}
