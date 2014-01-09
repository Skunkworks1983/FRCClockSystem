package com.skunk.clock.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

	public void connect() {
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"
					+ f.getAbsolutePath());
		} catch (SQLException e) {
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
		// ClocktimeDatabase cdb = new ClocktimeDatabase();
		// cdb.load(new File("data/time_chunk_07_01_2014.csv"), db, true);

		SQLiteAdapter dbAdapt = new SQLiteAdapter(new File("data/database.db"));
		dbAdapt.connect();
		// dbAdapt.saveMemberDatabase(db);

		dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921), -1L, -1L);
		dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921),
				1389142388390L, -1L);
		dbAdapt.loadClocktimeObjects(db.getMemberByUUID(4007921), -1L,
				1389142388500L);

		dbAdapt.disconnect();
	}

	public void saveMemberDatabase(MemberDatabase db) {
		try {
			Statement statement = connection.createStatement();

			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `members` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`name` varchar(50) NOT NULL,"
					+ "`type` varchar(10) NOT NULL,"
					+ "`groups` varchar(100) NOT NULL,"
					+ "`badge` varchar(25) NOT NULL,"
					+ "`image` varchar(25) NOT NULL," + "PRIMARY KEY (`uuid`)"
					+ ")");
			statement.close();

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

			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `members` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`name` varchar(50) NOT NULL,"
					+ "`type` varchar(10) NOT NULL,"
					+ "`groups` varchar(100) NOT NULL,"
					+ "`badge` varchar(25) NOT NULL,"
					+ "`image` varchar(25) NOT NULL," + "PRIMARY KEY (`uuid`)"
					+ ")");

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

	public void saveClocktimeObject(Member member, Clocktime clock) {
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `clocktimes` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`startime` int(12) NOT NULL,"
					+ "`endtime` int(12) NOT NULL)");
			statement.close();

			PreparedStatement insert = connection
					.prepareStatement("INSERT INTO `clocktimes` (`uuid`, `startime`, `endtime`) "
							+ "VALUES (?, ?, ?);");
			for (Entry<Long, Long> set : clock) {
				if (set.getKey() < set.getValue()) {
					insert.setLong(1, member.getUUID());
					insert.setLong(2, set.getKey());
					insert.setLong(3, set.getValue());
					insert.executeUpdate();
				}
			}
			insert.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	public List<Clocktime> loadClocktimeObjects(Member member, long start,
			long end) {
		List<Clocktime> clocktimes = new ArrayList<Clocktime>();
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `clocktimes` ("
					+ "`uuid` int(10) NOT NULL,"
					+ "`startime` int(12) NOT NULL,"
					+ "`endtime` int(12) NOT NULL)");
			System.out.println("SELECT * FROM `clocktimes`" + "WHERE `uuid`="
					+ member.getUUID()
					+ (start > 0 || end > 0 ? " AND ( " : "")
					+ (start > 0 ? "`startime`>" + start : "")
					+ (start > 0 && end > 0 ? " AND " : "")
					+ (end > 0 ? "`startime`<" + end : "")
					+ (start > 0 || end > 0 ? ")" : ""));
			ResultSet set = statement.executeQuery("SELECT * FROM `clocktimes`"
					+ "WHERE `uuid`=" + member.getUUID()
					+ (start > 0 || end > 0 ? " AND ( " : "")
					+ (start > 0 ? "`startime`>" + start : "")
					+ (start > 0 && end > 0 ? " AND " : "")
					+ (end > 0 ? "`startime`<" + end : "")
					+ (start > 0 || end > 0 ? ")" : ""));
			System.out.println(set.next());
			statement.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return clocktimes;
	}
}
