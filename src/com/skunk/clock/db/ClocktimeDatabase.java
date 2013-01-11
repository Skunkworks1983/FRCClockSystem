package com.skunk.clock.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.skunk.clock.Util;
import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;

public class ClocktimeDatabase {
	private final Map<Member, Clocktime> clocktimes;

	public ClocktimeDatabase() {
		this.clocktimes = new HashMap<Member, Clocktime>();
	}

	public int[] getClockedByType(MemberType... types) {
		int[] count = new int[MemberGroup.values().length + 1];
		for (Entry<Member, Clocktime> entry : clocktimes.entrySet()) {
			for (MemberType t : types) {
				if (entry.getKey().getType() == t
						&& entry.getValue().isClockedIn()) {
					count[0]++;
					for (MemberGroup g : entry.getKey().getGroups()) {
						count[g.ordinal() + 1]++;
					}
					break;
				}
			}
			if (types.length == 0) {
				for (MemberGroup g : entry.getKey().getGroups()) {
					count[g.ordinal() + 1]++;
				}
				count[0]++;
			}
		}
		return count;
	}

	public List<Entry<Member, Clocktime>> getClockedListByType(
			MemberType... types) {
		List<Entry<Member, Clocktime>> members = new ArrayList<Entry<Member, Clocktime>>();
		for (Entry<Member, Clocktime> entry : clocktimes.entrySet()) {
			for (MemberType t : types) {
				if (entry.getKey().getType() == t
						&& entry.getValue().isClockedIn()) {
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

	public void clockOutAllWith(int i) {
		for (Clocktime c : clocktimes.values()) {
			c.clockOutWith(i);
		}
	}

	public void save() throws IOException {
		String date = Util.formatDate(new Date());

		File totals = new File("data/time_total_" + date + ".csv");
		File clocks = new File("data/time_chunk_" + date + ".csv");

		BufferedWriter writeTotals = new BufferedWriter(new FileWriter(totals));
		BufferedWriter writeClocks = new BufferedWriter(new FileWriter(clocks));

		writeTotals.write("UUID,Time (millis),Time (minutes),Missing Badges");
		writeTotals.newLine();
		writeClocks.write("UUID,Chunk count, (Chunk Start, Chunk End)...");
		writeClocks.newLine();

		for (Entry<Member, Clocktime> clock : clocktimes.entrySet()) {
			if (clock.getValue().getClockTime() > 0) {
				writeTotals.write(clock.getKey().getUUID() + ","
						+ clock.getValue().getClockTime() + ","
						+ clock.getValue().getClockTime() / 60000 + ","
						+ clock.getValue().getMissingBadgeCount());
				writeTotals.newLine();
				writeClocks.write(clock.getKey().getUUID() + ","
						+ clock.getValue().getChunksString());
				writeClocks.newLine();
			}
		}
		System.out.println("Wrote " + clocktimes.size() + " records to '"
				+ clocks.getName() + "' and '" + totals.getName() + "'");
		writeTotals.close();
		writeClocks.close();
	}
}
