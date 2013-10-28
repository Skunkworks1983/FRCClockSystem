package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;

public class VisualizeDayContainer implements Container {
	private ClockWebServer server;

	public VisualizeDayContainer(ClockWebServer clockWebServer) {
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
			String file = data.get("file");
			if (file != null) {
				File dataFile = new File("./data/" + file);
				if (dataFile.isFile()) {
					ClocktimeDatabase db = server.getDatabase().getClocktime(
							dataFile.getAbsolutePath());
					if (db != null) {
						List<Entry<Member, Clocktime>> studentList = db
								.getListByType();
						body.println("<table border=1>");
						for (Entry<Member, Clocktime> entry : studentList) {
							if (entry.getValue().getClockTime() <= 0
									&& !data.containsKey("all")) {
								continue;
							}
							body.println("<tr><td>"
									+ entry.getKey().getName()
									+ "</td><td>"
									+ WebUtil.formatTimeLong(entry.getValue()
											.getClockTime()) + "</td></tr>");
						}
						body.println("</table>");
					}
				} else {
					body.println("No data file exists!");
				}
			} else {
				body.println("No file key!");
			}

			body.println("</body></html>");
			body.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
