package com.skunk.clock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
	private static DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy");

	public static String formatDate(Date d) {
		return dateFormat.format(d);
	}

	public static String formatTime(long clockTime) {
		int hours = (int) (clockTime / 3600000);
		int minutes = ((int) (clockTime / 60000) - (hours * 60));
		return hours + ":" + (minutes < 10 ? "0" : "") + minutes;
	}
}
