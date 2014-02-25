package com.skunk.clock.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;

public class SQLiteAdapter {
	private static String prepareMemberGroupString(MemberGroup... groups) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < groups.length; i++) {
			if (i > 0) {
				output.append(" ");
			}
			output.append(groups[i].name());
		}
		return output.toString();
	}

	private static MemberGroup[] decodeMemberGroupString(String... sGroups) {
		MemberGroup[] groups = new MemberGroup[sGroups.length];
		try {
			for (int i = 0; i < sGroups.length; i++) {
				groups[i] = MemberGroup.matchGroup(sGroups[i]);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid group, found "
					+ Arrays.toString(sGroups) + ", expecting one of "
					+ Arrays.toString(MemberGroup.values()));
			groups = new MemberGroup[] { MemberGroup.UNDEFINED };
		}
		return groups;
	}

	private Connection connection;
	private File f;

	public SQLiteAdapter(File f) {
		this.f = f;
	}

	private void ensureTables() throws SQLException {
		Statement statement = connection.createStatement();
		try {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `members` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`name` varchar(50) NOT NULL,"
					+ "`type` varchar(10) NOT NULL,"
					+ "`groups` varchar(100) NOT NULL,"
					+ "`badge` varchar(25) NOT NULL,"
					+ "`image` varchar(25) NOT NULL," + "PRIMARY KEY (`uuid`)"
					+ ")");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `clocktimes` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`start` datetime(12) NOT NULL,"
					+ "`end` datetime(12) NOT NULL,"
					+ "UNIQUE (`uuid`,`start`)" + ")");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `events` ("
					+ "`day` date(10) NOT NULL,"
					+ "`name` varchar(50) NOT NULL,"
					+ "`required` int(12) NOT NULL," + "PRIMARY KEY (`day`)"
					+ ")");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			statement.close();
		}
	}

	public void connect() {
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"
					+ f.getAbsolutePath());
			ensureTables();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
		}
	}

	public void disconnect() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		MemberDatabase db = new MemberDatabase();
		db.read();

		SQLiteAdapter dbAdapt = new SQLiteAdapter(new File("data/database.db"));
		dbAdapt.connect();
		{
			ClocktimeDatabase cdb = new ClocktimeDatabase();
			cdb.load(new File("data/time_chunk_07_01_2014.csv"), db, true);
			dbAdapt.saveClocktimeDB(cdb);
		}
		{
			ClocktimeDatabase cdb = new ClocktimeDatabase();
			cdb.load(new File("data/time_chunk_19_11_2013.csv"), db, true);
			dbAdapt.saveClocktimeDB(cdb);
		}
		// dbAdapt.saveMemberDatabase(db);
		/*
		 * { Clocktime clock = new Clocktime();
		 * clock.insertClockTime(1389142338390L, 1389142389390L);
		 * dbAdapt.saveClocktimeObject(db.getMemberByUUID(4007921), clock); }
		 */

		/*
		 * dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921), -1L, -1L);
		 * dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921),
		 * 1389142388390L, -1L);
		 * dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921), -1L,
		 * 1389142388500L);
		 */

		dbAdapt.disconnect();
	}

	public void saveMemberDatabase(MemberDatabase db) {
		try {
			PreparedStatement insert = connection
					.prepareStatement("INSERT OR REPLACE INTO `members` "
							+ "(`uuid`, `name`, `type`, `groups`, `badge`, `image`) "
							+ "VALUES (?, ?, ?, ?, ?, ?);");
			for (Member m : db) {
				insert.setLong(1, m.getUUID());
				insert.setString(2, m.getName());
				insert.setString(3, m.getType().name());
				insert.setString(4, prepareMemberGroupString(m.getGroups()));
				insert.setString(5, m.getBadge());
				insert.setString(6, m.getBadge());

				insert.executeUpdate();
			}
			insert.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	public void loadMemberDatabase(MemberDatabase db) {
		try {
			Statement statement = connection.createStatement();
			ResultSet results = statement
					.executeQuery("SELECT * FROM `members`;");
			while (results.next()) {
				try {
					MemberType type;
					try {
						type = MemberType.valueOf(results.getString(3)
								.toUpperCase());
					} catch (IllegalArgumentException e) {
						throw new ArrayIndexOutOfBoundsException(
								"Invalid type, found " + results.getString(3)
										+ ", expecting one of "
										+ Arrays.toString(MemberType.values())
										+ " ");
					}

					String[] sGroups = results.getString(4).split(" ");
					if (sGroups.length == 1 && sGroups[0].trim().length() == 0) {
						sGroups = new String[0];
					}
					MemberGroup[] groups = decodeMemberGroupString(sGroups);

					Member mem = new Member(results.getLong(1),
							results.getString(2), results.getString(6),
							results.getString(5), type, groups);
					db.insertMember(mem);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println(e.getMessage());
				}
			}
			statement.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	private void saveClocktimeObject(Member member, Clocktime clock) {
		try {
			PreparedStatement insert = connection
					.prepareStatement("INSERT INTO `clocktimes` (`uuid`, `start`, `end`) "
							+ "VALUES (?, ?, ?);");
			for (Entry<Long, Long> set : clock) {
				if (set.getKey() < set.getValue()) {
					insert.setLong(1, member.getUUID());
					insert.setTimestamp(2, new Timestamp(set.getKey()));
					insert.setTimestamp(3, new Timestamp(set.getValue()));
					insert.executeUpdate();
				}
			}
			insert.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	private List<Entry<Long, Long>> loadClocktimeObjects(Member member,
			long start, long end) {
		List<Entry<Long, Long>> clocktimes = new ArrayList<Entry<Long, Long>>();
		try {
			Statement statement = connection.createStatement();
			ResultSet set = statement.executeQuery("SELECT * FROM `clocktimes`"
					+ "WHERE `uuid`=" + member.getUUID()
					+ (start > 0 || end > 0 ? " AND ( " : "")
					+ (start > 0 ? "`start`>" + start : "")
					+ (start > 0 && end > 0 ? " AND " : "")
					+ (end > 0 ? "`start`<" + end : "")
					+ (start > 0 || end > 0 ? ")" : ""));
			while (set.next()) {
				clocktimes.add(new AbstractMap.SimpleEntry<Long, Long>(set
						.getTimestamp(1).getTime(), set.getTimestamp(2)
						.getTime()));
			}
			set.close();
			statement.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return clocktimes;
	}

	private void createEvent(Date time, String name, long required) {
		try {
			Date day = new Date(time.getTime());

			PreparedStatement insert = connection
					.prepareStatement("INSERT INTO `events` (`day`, `name`, `required`) "
							+ "VALUES (?, ?, ?);");
			insert.setDate(1, day);
			if (name != null) {
				insert.setString(2, name);
			} else {
				insert.setString(2, "");
			}
			insert.setLong(3, required);
			insert.executeUpdate();
			insert.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	public void saveClocktimeDB(ClocktimeDatabase db) {
		// Create event tag
		createEvent(new Date(db.getCreation()), db.getTag(),
				db.getRequiredTime());

		// Save clocktimes
		for (Entry<Member, Clocktime> ent : db.getListByType()) {
			saveClocktimeObject(ent.getKey(), ent.getValue());
		}
	}

	public ClocktimeDatabase loadClocktimeDB(MemberDatabase memDB, java.util.Date day) {
		// Ensure there are no time mistakes
		@SuppressWarnings("deprecation")
		Date start = new Date(day.getYear(), day.getMonth(), day.getDay());
		Date end = new Date(start.getTime() + (24 * 60 * 60 * 1000 - 1));

		ClocktimeDatabase clockDB = new ClocktimeDatabase();

		// Merge dat shit
		try {
			Statement statement = connection.createStatement();
			ResultSet set = statement.executeQuery("SELECT * FROM `clocktimes`"
					+ "WHERE `start`>=" + start.getTime() + " AND "
					+ "`start`<=" + end.getTime());
			while (set.next()) {
				clockDB.getClocktime(memDB.getMemberByUUID(set.getInt(1)))
						.insertClockTime(set.getTimestamp(2).getTime(),
								set.getTimestamp(3).getTime());
			}
			set.close();
			statement.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return clockDB;
	}
}
