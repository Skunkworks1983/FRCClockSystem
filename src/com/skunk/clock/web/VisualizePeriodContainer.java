package com.skunk.clock.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;

public class VisualizePeriodContainer implements Container {
	private ClockWebServer server;
	private static final long DEFAULT_LENGTH = 1000 * 60 * 60 * 24 * 7; // 1
																		// week

	public VisualizePeriodContainer(ClockWebServer clockWebServer) {
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

			body.println("<html><body>");

			Map<String, String> data = WebUtil.decodeGET(request.getTarget());
			String start = data.get("start");
			String end = data.get("end");
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
			if (start != null && end != null) {
				final long startTime = Long.valueOf(start);
				final long endTime = Long.valueOf(end) < 0 ? Long.MAX_VALUE
						: Long.valueOf(end);
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
				Map<Member, Clocktime> totalData = new HashMap<Member, Clocktime>();
				for (File f : list) {
					ClocktimeDatabase db = server.getDatabase().getClocktime(
							f.getAbsolutePath());
					for (Entry<Member, Clocktime> entry : db.getListByType()) {
						Clocktime supp = totalData.get(entry.getKey());
						if (supp == null) {
							supp = new Clocktime();
							totalData.put(entry.getKey(), supp);
						}
						supp.insertClockTime(entry.getValue());
					}
				}
				body.println("From " + WebUtil.formatDate(startTime) + " to "
						+ WebUtil.formatDate(endTime) + "</br>");
				if (totalData.size() != 0) {
					body.println("<table border=1>");
					for (Entry<Member, Clocktime> entry : totalData.entrySet()) {
						body.println("<tr><td>"
								+ entry.getKey().getName()
								+ "</td><td>"
								+ WebUtil.formatTimeLong(entry.getValue()
										.getClockTime()) + "</td></tr>");
					}
					body.println("</table>");
				}
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
