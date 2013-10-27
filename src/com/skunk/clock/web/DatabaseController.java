package com.skunk.clock.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.MemberDatabase;

public class DatabaseController {
	private MemberDatabase members;
	private Map<String, ClocktimeDatabase> oldDatabases = new HashMap<String, ClocktimeDatabase>();

	public MemberDatabase getMembers() {
		if (members == null) {
			members = new MemberDatabase();
			try {
				members.read();
			} catch (IOException e) {
			}
		}
		return members;
	}

	public ClocktimeDatabase getClocktime(String file) {
		file = new File(file).getAbsolutePath();
		ClocktimeDatabase db = oldDatabases.get(file);
		if (db == null) {
			db = new ClocktimeDatabase();
			try {
				db.load(new File(file), getMembers(), true);
			} catch (IOException e) {
			}
			oldDatabases.put(file, db);
		}
		return db;
	}
}
