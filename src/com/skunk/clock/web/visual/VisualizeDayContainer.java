package com.skunk.clock.web.visual;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.http.Request;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.web.APIHandler;
import com.skunk.clock.web.ClockWebServer;
import com.skunk.clock.web.WebUtil;

public class VisualizeDayContainer extends APIHandler {
	public VisualizeDayContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	public void appendContent(Request request, PrintStream body) {
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
	}
}
