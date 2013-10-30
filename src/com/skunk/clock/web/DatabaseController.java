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

	private Object membersLock = new Object();
	private Object databaseLock = new Object();

	public MemberDatabase getMembers() {
		synchronized (membersLock) {
			if (members == null) {
				members = new MemberDatabase();
				try {
					members.read();
				} catch (IOException e) {
				}
			}
			return members;
		}
	}

	public ClocktimeDatabase getClocktime(String file) {
		synchronized (databaseLock) {
			file = new File(file).getAbsolutePath();
			ClocktimeDatabase db = oldDatabases.get(file);
			if (db == null) {
				db = new ClocktimeDatabase();
				oldDatabases.put(file, db);
				try {
					db.load(new File(file), getMembers(), true);
				} catch (IOException e) {
				}
				System.out.println("Loaded database: " + file);
			}
			return db;
		}
	}
}
