package com.skunk.clock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public class Configuration {
	public static long CLOCK_UPDATE_TIME = 1000; // 1 second
	public static long CLOCKIN_DISPLAY_TIME = 5000; // 5 seconds
	public static int MUG_V_PADDING = 1;
	public static int MUG_H_PADDING = 1;
	public static int MUG_HEIGHT = 64;
	public static int MUG_WIDTH = 54;
	public static int MUG_CENTRAL_HEIGHT = MUG_HEIGHT;
	public static int MUG_CENTRAL_WIDTH = MUG_WIDTH;
	public static long CACHE_EXIPRY_TIME = 1000 * 60 * 60 * 3; // 3 hours
	public static int CENTER_WIDTH = 200;

	public static long SCREENSAVER_START_TIME = 30000;
	public static long SCREENSAVER_IMG_TIME = 10000;
	public static int NAME_LENGTH = 20;

	/**
	 * Loads the given file into the configuration. If the file doesn't exist it
	 * will also create a file with the default configuration.
	 * 
	 * @param f
	 *            the file to load
	 */
	public static void load(File f) {
		try {
			Field[] fields = Configuration.class.getFields();
			Properties props = new Properties();
			if (f.exists()) {
				FileInputStream in = new FileInputStream(f);
				props.load(in);
				in.close();
			} else {
				BufferedWriter writer = new BufferedWriter(new FileWriter(f));
				for (Field field : fields) {
					try {
						String val = field.get(null).toString();
						writer.write(field.getName() + ":" + val);
						writer.newLine();
					} catch (Exception e) {
						continue;
					}
				}
				writer.close();
			}

			for (Field field : fields) {
				try {
					if (field.getType() == long.class) {
						field.setLong(null, Long.valueOf(props.getProperty(
								field.getName(),
								String.valueOf(field.getLong(null)))));
					} else if (field.getType() == int.class) {
						field.setInt(
								null,
								Integer.valueOf(props.getProperty(
										field.getName(),
										String.valueOf(field.getInt(null)))));
					} else {
						field.set(
								null,
								field.getType().cast(
										props.getProperty(field.getName(),
												field.get(null).toString())));
					}
					props.put(field.getName(), field.get(null));
				} catch (Exception e) {
					System.out
							.println("Error with property " + field.getName());
				}
			}
		} catch (IOException e) {
		}
	}
}
