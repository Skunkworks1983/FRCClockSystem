package com.skunk.clock;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map.Entry;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberType;
import com.skunk.clock.gui.ClockGUI;

/**
 * @author westin
 */
public class Main {
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Dimension size = null;
		for (String s : args) {
			if (s.startsWith("size=")) {
				String[] chunks = s.substring(5).split("x");
				if (chunks.length == 2) {
					try {
						size = new Dimension(Integer.valueOf(chunks[0]),
								Integer.valueOf(chunks[1]));
					} catch (Exception e) {
					}
				}
			}
		}

		try {
			System.setOut(new PrintStream(new FileOutputStream(new File("log"))));
		} catch (Exception e) {
		}
		final ClockGUI frame = new ClockGUI();
		if (size != null) {
			frame.setSize(size);
		}
		frame.setVisible(true);
		frame.loadDB();

		// createCreepyServer(frame);

		while (frame.isVisible()) {
			frame.repaintLoop();
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * A useless and unused feature that allows people to send plaintext queries
	 * to the program to clock in and out.
	 * 
	 * @param frame
	 *            the frame to work with.          
	 */
	@SuppressWarnings("unused")
	private static void createCreepyServer(final ClockGUI frame) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket sock = new ServerSocket(3141);
					while (frame.isVisible()) {
						final Socket client = sock.accept();
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									BufferedReader read = new BufferedReader(
											new InputStreamReader(client
													.getInputStream()));
									BufferedWriter write = new BufferedWriter(
											new OutputStreamWriter(client
													.getOutputStream()));
									while (frame.isVisible()
											&& !client.isInputShutdown()) {
										String s = read.readLine();
										if (s == null)
											break;
										String[] chunks = s.split(" ");
										if (chunks.length >= 1
												&& chunks[0]
														.equalsIgnoreCase("students")) {
											write.write("--Students Clocked In--");
											write.newLine();
											List<Entry<Member, Clocktime>> cl = frame
													.getClock()
													.getClockedListByType(
															MemberType.STUDENT);
											for (Entry<Member, Clocktime> en : cl) {
												write.write(en.getKey()
														.getName());
												write.newLine();
											}
										} else if (chunks.length >= 1
												&& chunks[0]
														.equalsIgnoreCase("mentors")) {
											write.write("--Mentors Clocked In--");
											write.newLine();
											List<Entry<Member, Clocktime>> cl = frame
													.getClock()
													.getClockedListByType(
															MemberType.MENTOR,
															MemberType.COACH);
											for (Entry<Member, Clocktime> en : cl) {
												write.write(en.getKey()
														.getName());
												write.newLine();
											}
										} else {
											try {
												Long.valueOf(chunks[0]
														.toUpperCase().replace(
																"S", ""));
												String[] info = frame
														.clockInOut(chunks[0],
																false);
												write.write(info[0] + "\t"
														+ info[1] + "\t"
														+ info[2]);
												write.newLine();
											} catch (Exception e) {
												write.write("--HELP--");
												write.newLine();
												write.write("students\tLists clocked in students.");
												write.newLine();
												write.write("mentors\tLists clocked in mentors.");
												write.newLine();
												write.write("<UUID>\tClocks a user in or out");
												write.newLine();
											}
										}
										write.flush();
									}
									write.close();
									read.close();
									client.close();
								} catch (Exception e) {
								}
							}
						}).start();
					}
					sock.close();
				} catch (Exception e) {
				}
			}
		}).start();
	}
}
