package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.net.URLEncoder;

import org.simpleframework.http.Request;

public class CurrentDataContainer extends APIHandler {
	public CurrentDataContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request request, PrintStream body)
			throws Exception {
		body.println("<html><body>");
		// Whip out the files list
		File dir = new File("./data");
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if (f.isFile() && f.getName().startsWith("time_chunk_")) {
					body.println("<a href='/visual/day?file="
							+ URLEncoder.encode(f.getName(), "UTF-8") + "'>"
							+ f.getName() + "</a></br>");
				}
			}
		}
		body.println("</body></html>");
	}

}
