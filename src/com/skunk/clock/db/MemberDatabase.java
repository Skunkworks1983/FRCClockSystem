package com.skunk.clock.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;

/**
 * @author westin
 */
public class MemberDatabase implements Iterable<Member> {
	private final Map<Long, Member> membersByUUID;
	private final Map<String, Member> membersByBadge;

	public MemberDatabase() {
		this.membersByUUID = new HashMap<Long, Member>();
		this.membersByBadge = new HashMap<String, Member>();
	}

	public Member getMemberByUUID(long uuid) {
		return membersByUUID.get(uuid);
	}

	public Member getMemberByBadge(String badge) {
		return membersByBadge.get(badge);
	}

	public void read() throws IOException {
		File f = new File("data/members.csv");
		BufferedReader read = new BufferedReader(new FileReader(f));
		int currentLine = 0;
		membersByUUID.clear();
		membersByBadge.clear();
		while (true) {
			String line = read.readLine();
			if (line == null) {
				break;
			}
			currentLine++; // Debug
	
			String[] chunks = line.split(",");
			try {
				if (chunks.length < 6) {
					throw new ArrayIndexOutOfBoundsException(
							"Not enough array elements, found " + chunks.length
									+ ", expecting 6 ");
				}
				long uuid = Long.valueOf(chunks[0]);
				MemberType type;
				try {
					type = MemberType.valueOf(chunks[1].toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new ArrayIndexOutOfBoundsException(
							"Invalid type, found " + chunks[2]
									+ ", expecting one of "
									+ Arrays.toString(MemberType.values())
									+ " ");
				}
				
				String[] sGroups = chunks[3].split(" ");
				if (sGroups.length == 1 && sGroups[0].trim().length() == 0) {
					sGroups = new String[0];
				}
				MemberGroup[] groups = new MemberGroup[sGroups.length];
				try {
					for (int i = 0; i < sGroups.length; i++) {
						groups[i] = MemberGroup.matchGroup(sGroups[i]);
					}
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid group, found " + chunks[3]
							+ ", expecting one of "
							+ Arrays.toString(MemberGroup.values()) + " "
							+ " on line " + currentLine
							+ " of 'data/members.csv'");
					groups = new MemberGroup[] { MemberGroup.UNDEFINED };
				}
				
				Member mem = new Member(uuid, chunks[2], chunks[4], chunks[5],
						type, groups);
				membersByUUID.put(uuid, mem);
				membersByBadge.put(mem.getBadge(), mem);
			} catch (NumberFormatException e) {
				System.out.println("Number format error on line " + currentLine
						+ " of 'data/members.csv'");
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(e.getMessage() + " on line " + currentLine
						+ " of 'data/members.csv'");
			}
		}
		System.out.println("Read " + membersByUUID.size() + "/" + currentLine
				+ " records from '" + f.getName() + "'");
		read.close();
	}

	@Override
	public Iterator<Member> iterator() {
		return membersByUUID.values().iterator();
	}

	public int size() {
		return membersByUUID.size();
	}
}
