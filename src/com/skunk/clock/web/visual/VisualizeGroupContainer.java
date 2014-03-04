package com.skunk.clock.web.visual;

import java.io.PrintStream;

import org.simpleframework.http.Request;

import com.skunk.clock.db.Member;
import com.skunk.clock.web.APIHandler;
import com.skunk.clock.web.ClockWebServer;

public class VisualizeGroupContainer extends APIHandler {
	public VisualizeGroupContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request request, PrintStream body) {

		body.println("<html><body>");

		body.println("<table id='greenTable'>");
		for (Member m : server.getDatabase().getMembers()) {
			body.println("<tr>");
			body.println("<td>"+ m.getName() + "</td>");
			body.println("<td>" + (m.getGroups().length >0?m.getGroups()[0].formattedName():"") + "</td>");
			body.println("</tr>");
		}
		body.println("</table>");

		body.println("</body></html>");
	}
}
