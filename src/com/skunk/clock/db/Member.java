package com.skunk.clock.db;

import java.util.Arrays;

/**
 * @author westin
 * 
 */
public class Member {
	private final String name;
	private final Long uuid;
	private final MemberType type;
	private final MemberGroup[] groups;
	private final String badge, img;

	public static enum MemberType {
		STUDENT, MENTOR, COACH, ADMIN;
		public String formattedName() {
			return name().toUpperCase().substring(0, 1)
					+ name().toLowerCase().substring(1);
		}
	}

	public static enum MemberGroup {
		SYSTEMS, PROGRAMMING, BUILD, MARKETING, ELECTRICAL, MENTOR, DESIGN, SAFETY, LEAD, SUBLEAD, DATA, OUTREACH, UNDEFINED;
		public String formattedName() {
			return name().toUpperCase().substring(0, 1)
					+ name().toLowerCase().substring(1);
		}

		public static MemberGroup matchGroup(String s) {
			for (MemberGroup g : values()) {
				if (g.name().equalsIgnoreCase(s)) {
					return g;
				}
			}
			throw new IllegalArgumentException("");
		}
	}

	Member(long uuid, String name, String img, String badge, MemberType type,
			MemberGroup[] group) {
		this.name = name;
		this.uuid = uuid;
		this.type = type;
		if (group.length == 0) {
			this.groups = new MemberGroup[] { MemberGroup.UNDEFINED };
		} else {
			this.groups = group;
		}
		this.img = img;
		this.badge = badge;
	}

	public String getReversedName() {
		int space = name.indexOf(' ');
		if (space > 0) {
			return name.substring(space + 1).concat(name.substring(0, space));
		} else {
			return name;
		}
	}

	public String getBadge() {
		return badge;
	}

	public String getIMG() {
		return img;
	}

	public MemberGroup[] getDisplayedGroups() {
		return new MemberGroup[] { groups[0] };
	}

	public MemberGroup[] getGroups() {
		return groups;
	}

	public MemberType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public long getUUID() {
		return uuid;
	}

	public int hashCode() {
		return uuid.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Member) {
			return ((Member) o).getUUID() == getUUID();
		}
		return false;
	}

	public String toString() {
		return getClass().getSimpleName() + "[uuid=" + getUUID() + ",type="
				+ getType().formattedName() + ",groups="
				+ Arrays.toString(getDisplayedGroups()) + "]";
	}

	public boolean isInGroup(MemberGroup test) {
		for (MemberGroup g : groups) {
			if (test == g) {
				return true;
			}
		}
		return false;
	}
}
