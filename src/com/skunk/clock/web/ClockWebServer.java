package com.skunk.clock.web;

import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class ClockWebServer implements Container {
	private Map<String, Container> containers = new HashMap<String, Container>();
	private DatabaseController db;

	public DatabaseController getDatabase() {
		return db;
	}

	private ClockWebServer() {
		containers.put("", new CurrentDataContainer(this));
		containers.put("visual/day", new VisualizeDayContainer(this));
		containers.put("visual/period", new VisualizePeriodContainer(this));
		containers.put("visual/user", new VisualizeUserContainer(this));
		containers.put("visual/user/image", new GetUserImage(this));
		db = new DatabaseController();
	}

	public void handle(Request request, Response response) {
		String targetClean = request.getTarget().toLowerCase();
		if (targetClean.indexOf('?') >= 0) {
			targetClean = targetClean.substring(0, targetClean.indexOf('?'));
		}
		if (targetClean.startsWith("/")) {
			targetClean = targetClean.substring(1);
		}
		if (targetClean.endsWith("/")) {
			targetClean = targetClean.substring(0, targetClean.length() - 1);
		}
		Container obj;
		try {
			obj = containers.get(URLDecoder.decode(targetClean.trim(), "UTF-8")
					.toLowerCase());
			System.out.println("Request: "
					+ request.getTarget()
					+ " ("
					+ URLDecoder.decode(targetClean.trim(), "UTF-8")
							.toLowerCase() + ") -> "
					+ (obj != null ? obj.getClass().getSimpleName() : "null"));
			if (obj != null) {
				obj.handle(request, response);
				return;
			} else {
				try {
					PrintStream body = response.getPrintStream();
					response.setCode(404);
					body.println("Not found: "
							+ URLDecoder.decode(targetClean.trim(), "UTF-8")
									.toLowerCase());
				} catch (Exception e2) {
				}
			}
		} catch (Exception e) {
			try {
				PrintStream body = response.getPrintStream();
				response.setCode(503);
				e.printStackTrace(body);
			} catch (Exception e2) {
			}
		}
		try {
			response.getPrintStream().close();
		} catch (Exception e) {
		}
	}

	public static void main(String[] list) throws Exception {
		Container container = new ClockWebServer();
		Server server = new ContainerServer(container);
		Connection connection = new SocketConnection(server);
		SocketAddress address = new InetSocketAddress(8080);
		connection.connect(address);
	}
}