package com.skunk.clock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
		final ClockGUI frame = new ClockGUI();
		frame.setVisible(true);
		frame.loadDB();
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
										if (chunks.length >= 2
												&& chunks[0]
														.equalsIgnoreCase("login")) {
											String[] info = frame.login(
													chunks[1], false);
											write.write(info[0] + "\t"
													+ info[1] + "\t" + info[2]);
											write.newLine();
										} else if (chunks.length >= 1
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
											write.write("--HELP--");
											write.newLine();
											write.write("students\tLists clocked in students.");
											write.newLine();
											write.write("mentors\tLists clocked in mentors.");
											write.newLine();
											write.write("login <UUID>\tClocks a user in or out");
											write.newLine();
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
		while (frame.isVisible()) {
			frame.repaintLoop();
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
		}
	}
}