package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
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

			Map<String, String> data = WebUtil.decodeGET(request.getTarget());
			String start = data.get("start");
			String end = data.get("end");
			String uuid = data.get("uuid");

			if (uuid == null) {
				body.println("<link rel=\"stylesheet\" href=\"/data/resources/datepickr.css\" type=\"text/css\" media=\"screen\">");
				body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/datepickr.min.js\"></script>");
				body.println("</head><body>");
				body.println("<form method='GET' action=''>");
				body.println("User: <input type='text' name='uuid'/></br>");
				body.println("Start: <input type='text' name='start' id='dateEnterA'/><span style='padding: 0px 25px;'>|</span>");
				body.println("End: <input type='text' name='end' id='dateEnterB'/><span style='padding: 0px 25px;'>|</span>");
				body.println("<input type='submit'/></form>");
				body.println("<script type='text/javascript'>new datepickr('dateEnterB', {'dateFormat': 's'});new datepickr('dateEnterA', {'dateFormat': 's'});</script>");
			} else {
				body.println("<link rel=\"stylesheet\" href=\"/data/resources/chart.css\" type=\"text/css\" media=\"screen\">");
				body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/raphael.js\"></script><script src=\"/data/resources/popup.js\"></script><script src=\"/data/resources/jquery.js\"></script><script src=\"/data/resources/user_graph.js\"></script>");
				body.println("</head><body>");

				if (start == null && end != null) {
					start = String.valueOf(Long.valueOf(end) - DEFAULT_LENGTH);
				} else if (start != null && end == null) {
					end = String.valueOf(Long.valueOf(start) + DEFAULT_LENGTH);
				} else if (start == null && end == null) {
					Calendar today = Calendar.getInstance();
					today.set(Calendar.getInstance().get(Calendar.YEAR),
							Calendar.getInstance().get(Calendar.MONTH),
							Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
							0, 0, 0);
					start = String.valueOf(today.getTimeInMillis()
							- DEFAULT_LENGTH);
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
					File[] list = WebUtil.listOrderedDatabase(startTime,
							endTime);
					long[][] timeHeaders = new long[list.length][2];
					int i = 0;
					for (File f : list) {
						ClocktimeDatabase db = server.getDatabase()
								.getClocktime(f.getAbsolutePath());
						Clocktime info = db.getClocktime(mem);
						if (info != null) {
							timeHeaders[i][0] = db.getCreation();
							timeHeaders[i][1] = info.getClockTime();
							i++;
						} else {
							timeHeaders[i][0] = Long.MAX_VALUE;
							i++;
						}
					}

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
			}
			body.println("</body></html>");
			body.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
