package com.skunk.clock.web;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

public abstract class APIHandler implements Container {
	protected final ClockWebServer server;

	protected APIHandler(ClockWebServer clockWebServer) {
		this.server = clockWebServer;
	}

	public abstract void appendContent(Request r, PrintStream ss)
			throws Exception;

	@Override
	public final void handle(Request request, Response response) {
		try {
			final PrintStream body = response.getPrintStream();
			long time = System.currentTimeMillis();

			response.setValue("Content-Type", "text/html");
			response.setValue("Server", "SimpleHTTP/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			try {
				appendContent(request, body);
			} catch (Exception e) {
				body.println("<font style='font-family: \"Courier New\", Courier, monospace; color: red;'>");
				e.printStackTrace(new PrintWriter(new Writer() {
					@Override
					public void write(char[] cbuf, int off, int len)
							throws IOException {
						body.println(new String(cbuf, off, len).replace("\n",
								"</br>\n"));
					}

					@Override
					public void flush() throws IOException {
						body.flush();
					}

					@Override
					public void close() throws IOException {
						body.close();
					}
				}));
				body.println("</font>");
				body.println("<hr>");
			}

			body.println("Generated in: " + (System.currentTimeMillis() - time)
					+ " ms</br>");
			body.println("Reports updated: "
					+ WebUtil.formatDateTime(WebUtil.getLastDBUpdate()));
			body.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
