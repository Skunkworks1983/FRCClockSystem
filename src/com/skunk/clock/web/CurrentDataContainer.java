package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.net.URLEncoder;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

public class CurrentDataContainer implements Container {
	private ClockWebServer server;

	public CurrentDataContainer(ClockWebServer clockWebServer) {
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
			// Whip out the files list
			File dir = new File("./data");
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					if (f.isFile() && f.getName().startsWith("time_chunk_")) {
						body.println("<a href='/visual/day?file="
								+ URLEncoder.encode(f.getName(), "UTF-8")
								+ "'>" + f.getName() + "</a></br>");
					}
				}
			}
			body.println("</body></html>");
			body.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
