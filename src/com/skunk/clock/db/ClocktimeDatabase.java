package com.skunk.clock.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.skunk.clock.Configuration;
import com.skunk.clock.Util;
import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;

public class ClocktimeDatabase {
	private final Map<Member, Clocktime> clocktimes;
	private long creation;
	private long requiredTime = (long) (1000 * 60 * 60 * 3);
	private long modified;
	private String tag = null;

	public ClocktimeDatabase() {
		this.clocktimes = new HashMap<Member, Clocktime>();
		modified = creation = System.currentTimeMillis();
	}

	public long getCreation() {
		return creation;
	}

	public long getModified() {
		return modified;
	}

	/**
	 * Returns the number of people that are clocked in of any of the types
	 * provided.
	 * 
	 * @param types
	 *            The types to count.
	 * @return an array of numbers mapped to the indices of the member groups.
	 *         The first one is the total.
	 */
	public int[] getClockedByType(MemberType... types) {
		int[] count = new int[MemberGroup.values().length + 1];
		for (Entry<Member, Clocktime> entry : clocktimes.entrySet()) {
			if (!entry.getValue().isClockedIn()) {
				continue;
			}
			for (MemberType t : types) {
				if (entry.getKey().getType() == t) {
					count[0]++;
					for (MemberGroup g : entry.getKey().getDisplayedGroups()) {
						count[g.ordinal() + 1]++;
					}
					break;
				}
			}
			if (types.length == 0) {
				for (MemberGroup g : entry.getKey().getDisplayedGroups()) {
					count[g.ordinal() + 1]++;
				}
				count[0]++;
			}
		}
		return count;
	}

	/**
	 * Grabs a list of everyone clocked in by their member type. If no types are
	 * set it returns all the clocked in members.
	 * 
	 * @param types
	 * @return the list of members mapped to clocktimes
	 */
	public List<Entry<Member, Clocktime>> getClockedListByType(
			MemberType... types) {
		List<Entry<Member, Clocktime>> members = new ArrayList<Entry<Member, Clocktime>>();
		for (Entry<Member, Clocktime> entry : clocktimes.entrySet()) {
			if (!entry.getValue().isClockedIn()) {
				continue;
			}
			for (MemberType t : types) {
				if (entry.getKey().getType() == t) {
					members.add(entry);
					break;
				}
			}
			if (types.length == 0) {
				members.add(entry);
			}
		}
		return members;
	}

	/**
	 * Grabs a list of everyone added to this database by their member type. If
	 * no types are set it returns all the clocked in members.
	 * 
	 * @param types
	 * @return the list of members mapped to clocktimes
	 */
	public List<Entry<Member, Clocktime>> getListByType(MemberType... types) {
		List<Entry<Member, Clocktime>> members = new ArrayList<Entry<Member, Clocktime>>();
		for (Entry<Member, Clocktime> entry : clocktimes.entrySet()) {
			for (MemberType t : types) {
				if (entry.getKey().getType() == t) {
					members.add(entry);
					break;
				}
			}
			if (types.length == 0) {
				members.add(entry);
			}
		}
		return members;
	}

	/**
	 * Sets that this database was just modified. Updates the modified time to
	 * the current time.
	 */
	public void modifyCall() {
		modified = System.currentTimeMillis();
	}

	public Clocktime getClocktime(Member mem) {
		Clocktime clock = clocktimes.get(mem);
		if (clock == null) {
			clock = new Clocktime();
			clocktimes.put(mem, clock);
		}
		return clock;
	}

	public void clockOutAll() {
		for (Clocktime c : clocktimes.values()) {
			c.clockOut(true);
		}
	}

	/**
	 * Clocks out all clocked-in members with the given amount of time in the
	 * last clock-time chunk.
	 * 
	 * @param time
	 *            the time in milliseconds
	 */
	public void clockOutAllWith(long time) {
		for (Clocktime c : clocktimes.values()) {
			c.clockOutWith(time);
		}
	}

	/**
	 * Saves both the overall data file and the raw chunk file with the full
	 * date prefixes.
	 * 
	 * @throws IOException
	 *             if an error occurs
	 */
	public void save() throws IOException {
		String date = Util.formatDate(new Date());

		File totals = new File("data/time_total_" + date + ".csv");
		File clocks = new File("data/time_chunk_" + date + ".csv");

		FileOutputStream totalsFout = new FileOutputStream(totals);
		FileOutputStream clocksFout = new FileOutputStream(clocks);

		BufferedWriter writeTotals = new BufferedWriter(new OutputStreamWriter(
				totalsFout));

		writeTotals.write("UUID,Time (millis),Time (minutes),Missing Badges");
		writeTotals.newLine();

		for (Entry<Member, Clocktime> clock : clocktimes.entrySet()) {
			if (clock.getValue().getClockTime() > 0) {
				writeTotals.write(clock.getKey().getUUID() + ","
						+ clock.getValue().getClockTime() + ","
						+ clock.getValue().getClockTime() / (60 * 1000) + ","
						+ clock.getValue().getMissingBadgeCount());
				writeTotals.newLine();
			}
		}
		writeTotals.flush();

		writeRawData(clocksFout, true);

		try {
			clocksFout.flush();
			clocksFout.getFD().sync();
		} catch (SyncFailedException e) {
			System.out.println(e.getMessage());
		}
		clocksFout.close();

		try {
			totalsFout.flush();
			totalsFout.getFD().sync();
		} catch (SyncFailedException e) {
			System.out.println(e.getMessage());
		}
		totalsFout.close();

		System.out.println("Wrote " + clocktimes.size() + " records to '"
				+ clocks.getName() + "' and '" + totals.getName() + "'");
		new File("data/cached.csv").delete();
	}

	/**
	 * Writes raw clock information to the given file.
	 * 
	 * @param f
	 *            the file to write information to
	 * @param header
	 *            if a column header should be written.
	 * @throws IOException
	 *             if an error occurs
	 */
	private void writeRawData(FileOutputStream f, boolean header)
			throws IOException {
		BufferedWriter writeClocks = new BufferedWriter(new OutputStreamWriter(
				f));
		if (creation < 0) {
			creation = System.currentTimeMillis();
		}
		writeClocks.write(String.valueOf(creation)
				+ ":"
				+ String.valueOf(getRequiredTime()
						+ (tag != null ? ":" + String.valueOf(tag) : "")));
		writeClocks.newLine();
		if (header) {
			writeClocks
					.write("UUID, Missing Badges, Chunk count, (Chunk Start, Chunk End)...");
			writeClocks.newLine();
		}
		for (Entry<Member, Clocktime> clock : clocktimes.entrySet()) {
			writeClocks.write(clock.getKey().getUUID() + ","
					+ clock.getValue().getMissingBadgeCount() + ","
					+ clock.getValue().getChunksString());
			writeClocks.newLine();
		}
		writeClocks.flush();
	}

	/**
	 * Saves the current raw data in the cached data file, for bootup-recovery.
	 * Also moves the old cache file to 'cached.csv.old'.
	 * 
	 * @throws IOException
	 *             if an error occurs
	 */
	public void cachedSave(File oldClocks) throws IOException {
		File tmpPath = new File(oldClocks.getAbsolutePath().concat(".tmp"));
		File finalPath = new File(oldClocks.getAbsolutePath());
		File oldPath = new File(oldClocks.getAbsolutePath().concat(".old"));

		FileOutputStream fOut = new FileOutputStream(tmpPath);
		writeRawData(fOut, false);

		fOut.flush();
		try {
			fOut.getFD().sync();
		} catch (SyncFailedException e) {
			System.out.println(e.getMessage());
		}
		fOut.close();

		if (oldClocks.exists()) {
			System.out.println("Moving old cached data: "
					+ oldClocks.renameTo(oldPath));
		}
		System.out.println("Moving new cached data: "
				+ tmpPath.renameTo(finalPath));
	}

	/**
	 * Loads the clock time database from the last created cache file, if it
	 * exists.
	 * 
	 * @param memDB
	 *            the member database used to lookup IDs
	 * @throws IOException
	 *             if an error occurs
	 */
	public void load(File clocks, MemberDatabase memDB, boolean ignoreTimestamp)
			throws IOException {
		if (clocks.exists()) {
			System.out.println("Found cached state... loading");
			BufferedReader reader = new BufferedReader(new FileReader(clocks));
			long timeStamp = -1;
			try {
				String[] header = reader.readLine().split(":");
				timeStamp = Long.valueOf(header[0]);
				int tagHead = 1;
				if (header.length > 1) {
					try {
						long testTime = Long.valueOf(header[1]);
						tagHead = 2;
						if (testTime > 0) {
							requiredTime = testTime;
						}
					} catch (NumberFormatException e) {
					}
				}
				if (header.length > tagHead) {
					tag = header[tagHead].trim();
				}
				if (!ignoreTimestamp
						&& System.currentTimeMillis() - timeStamp > Configuration.CACHE_EXIPRY_TIME) {
					System.out.println("...but the cached state has expired.");
					timeStamp = -1;
				} else {
					creation = timeStamp;
				}
			} catch (Exception e) {
				System.out
						.println("...but the cached state has an invalid timestamp.");
				timeStamp = -1;
			}
			while (timeStamp != -1) {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				String[] chunks = s.split(",");
				if (chunks.length > 2) {
					try {
						long uuid = Long.valueOf(chunks[0]);
						Member mem = memDB.getMemberByUUID(uuid);
						if (mem != null) {
							Clocktime c = new Clocktime();
							c.setMissingBadges(Integer.valueOf(chunks[1]));
							for (int i = 4; i < chunks.length; i += 2) {
								long start = Long.valueOf(chunks[i - 1]);
								long end = Long.valueOf(chunks[i]);
								c.insertClockTime(start, end);
							}
							clocktimes.put(mem, c);
						}
					} catch (Exception e) {
					}
				}
			}
			reader.close();
			System.out.println("Loaded " + clocktimes.size() + " times.");
		}
	}

	public long getRequiredTime() {
		return requiredTime;
	}

	public String getTag() {
		return tag;
	}

	public int getEntryCount() {
		return clocktimes.size();
	}
}
