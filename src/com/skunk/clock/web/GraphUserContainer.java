package com.skunk.clock.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;

public class GraphUserContainer implements Container {
	private ClockWebServer server;
	private static final long DEFAULT_LENGTH = 1000 * 60 * 60 * 24 * 7; // 1
																		// week

	public GraphUserContainer(ClockWebServer clockWebServer) {
		this.server = clockWebServer;
	}

	@Override
	public void handle(Request request, Response response) {
		try {
			PrintStream body = response.getPrintStream();
			long time = System.currentTimeMillis();

			response.setValue("Content-Type", "text/html");
			response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			body.println("<html><head>");
			body.println("<link rel=\"stylesheet\" href=\"/data/resources/chart.css\" type=\"text/css\" media=\"screen\">");
			body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/raphael.js\"></script><script src=\"/data/resources/popup.js\"></script><script src=\"/data/resources/jquery.js\"></script><script src=\"/data/resources/user_graph.js\"></script>");
			body.println("</head><body>");

			Map<String, String> data = WebUtil.decodeGET(request.getTarget());
			String start = data.get("start");
			String end = data.get("end");
			String uuid = data.get("uuid");

			if (start == null && end != null) {
				start = String.valueOf(Long.valueOf(end) - DEFAULT_LENGTH);
			} else if (start != null && end == null) {
				end = String.valueOf(Long.valueOf(start) + DEFAULT_LENGTH);
			} else if (start == null && end == null) {
				Calendar today = Calendar.getInstance();
				today.set(Calendar.getInstance().get(Calendar.YEAR), Calendar
						.getInstance().get(Calendar.MONTH), Calendar
						.getInstance().get(Calendar.DAY_OF_MONTH), 0, 0, 0);
				start = String
						.valueOf(today.getTimeInMillis() - DEFAULT_LENGTH);
				end = String.valueOf(today.getTimeInMillis()
						+ (24 * 60 * 60 * 1000) - 1);
			}
			if (start != null && end != null && uuid != null) {
				final long startTime = Long.valueOf(start);
				final long endTime = Long.valueOf(end) < 0 ? Long.MAX_VALUE
						: Long.valueOf(end);
				Member mem = server.getDatabase().getMembers()
						.getMemberByBadge(uuid);
				if (mem == null) {
					try {
						mem = server.getDatabase().getMembers()
								.getMemberByUUID(Long.valueOf(uuid));
					} catch (Exception e) {
					}
				}
				File[] list = new File("./data")
						.listFiles(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								if (name.startsWith("time_chunk_")) {
									// Parse as a date
									String[] dateInfo = name.substring(11,
											name.length() - 4).split("_");
									if (dateInfo.length == 3) {
										Calendar cal = Calendar.getInstance();
										cal.set(Integer.valueOf(dateInfo[2]),
												Integer.valueOf(dateInfo[1]) - 1,
												Integer.valueOf(dateInfo[0]),
												0, 0, 0);
										if (cal.getTimeInMillis() >= startTime
												&& cal.getTimeInMillis() <= endTime) {
											return true;
										}
									}
								}
								return false;
							}
						});
				long[][] timeHeaders = new long[list.length][2];
				int i = 0;
				for (File f : list) {
					ClocktimeDatabase db = server.getDatabase().getClocktime(
							f.getAbsolutePath());
					Clocktime info = db.getClocktime(mem);
					if (info != null) {
						timeHeaders[i][0] = db.getCreation();
						timeHeaders[i][1] = info.getClockTime();
						i++;
					}
				}
				Arrays.sort(timeHeaders, new Comparator<long[]>() {
					@Override
					public int compare(long[] o1, long[] o2) {
						return (int) (o1[0] - o2[0]);
					}
				});

				body.println("<table id=data>");
				body.println("<tfoot><tr>");
				for (int j = 0; j < i; j++) {
					body.println("<th>" + timeHeaders[j][0] + "</th>");
				}
				body.println("</tr></tfoot>");
				body.println("<tbody><tr>");
				for (int j = 0; j < i; j++) {
					body.println("<td>" + timeHeaders[j][1] + "</td>");
				}
				body.println("</tr></tbody>");
				body.println("</table>");
				body.println("<div id='title'>" + mem.getName() + "</div>");
				body.println("<div id='holder'></div>");
			} else {
				body.println("Requires a start and end key!");
			}

			body.println("</body></html>");
			body.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
