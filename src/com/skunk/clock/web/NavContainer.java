package com.skunk.clock.web;

import java.io.PrintStream;

import org.simpleframework.http.Request;

public class NavContainer extends APIHandler {
	protected NavContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request r, PrintStream ss) throws Exception {
		ss.println("<html><body>");
		ss.println("<a href='/list'>Show Recorded Days</a></br>");
		ss.println("<a href='/visual/period'>Show Period's Attendence</a></br>");
		ss.println("<a href='/graph/user'>Show User's Attendence</a></br>");
		ss.println("</body></html>");
	}
}
