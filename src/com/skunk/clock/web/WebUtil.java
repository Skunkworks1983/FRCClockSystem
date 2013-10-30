package com.skunk.clock.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

	public static String formatPercentage(long time, long totalTime) {
		double perc = (double) time * 100D / (double) totalTime;
		return ((int) (perc * 10) / 10f) + "%";
	}

	@SuppressWarnings("deprecation")
	public static File[] listOrderedDatabase(final long startTime,
			final long endTime) {
		final Calendar cal = Calendar.getInstance();
		final Map<String, Long> timemap = new HashMap<String, Long>();
		final File[] list = new File("./data").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith("time_chunk_")) {
					// Parse as a date
					String[] dateInfo = name.substring(11, name.length() - 4)
							.split("_");
					if (dateInfo.length == 3) {
						cal.set(Integer.valueOf(dateInfo[2]),
								Integer.valueOf(dateInfo[1]) - 1,
								Integer.valueOf(dateInfo[0]), 0, 0, 0);
						timemap.put(name, cal.getTimeInMillis());
						if (cal.getTimeInMillis() >= startTime
								&& cal.getTimeInMillis() <= endTime) {
							return true;
						}
					}
				}
				return false;
			}
		});
		System.out.println("#sorting.");
		System.out.flush();
		AtomicBoolean sort = new AtomicBoolean(false);
		int i = 0;
		while (sort.get() == false && i++ < 5) {
			Thread d = new Thread() {
				public void run() {
					Arrays.sort(list, new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							long x = timemap.get(o1.getName());
							long y = timemap.get(o2.getName());
							return (x < y) ? -1 : ((x == y) ? 0 : 1);
						}
					});
				}
			};
			d.start();
			try {
				d.join(5000);
				sort.set(true);
			} catch (InterruptedException e) {
				d.stop();
			}
		}
		System.out.println("#sorted: " + sort.get());
		System.out.flush();
		return list;
	}
}
