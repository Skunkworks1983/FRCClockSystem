package com.skunk.clock.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WebUtil {

	public static Map<String, String> decodeGET(String target) {
		Map<String, String> get = new HashMap<String, String>();
		if (target.indexOf('?') > 0) {
			String info = target.substring(target.indexOf('?') + 1);
			String[] parts = info.split("&");
			for (int i = 0; i < parts.length; i++) {
				String[] temp = parts[i].split("=", 2);
				try {
					if (temp.length == 2) {
						get.put(URLDecoder.decode(temp[0], "UTF-8"),
								URLDecoder.decode(temp[1], "UTF-8"));
					} else {
						get.put(URLDecoder.decode(temp[0], "UTF-8"), "");
					}
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		return get;
	}

	public static String formatTimeLong(long millis) {
		int minutes = (int) (millis / 60 / 1000);
		int hours = minutes / 60;
		minutes -= hours * 60;
		return hours + " hrs, " + minutes + " mins";
	}

	public static String formatDate(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		return DateFormat.getDateInstance().format(cal.getTime());
	}
}
