package com.skunk.clock.web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import javax.imageio.ImageIO;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import com.skunk.clock.db.Member;

public class GetUserImage implements Container {
	private ClockWebServer server;

	public GetUserImage(ClockWebServer clockWebServer) {
		this.server = clockWebServer;
	}

	@Override
	public void handle(Request request, Response response) {
		try {
			OutputStream rawBody = response.getOutputStream();
			PrintStream body = response.getPrintStream();
			long time = System.currentTimeMillis();

			response.setValue("Content-Type", "image/png");
			response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			Map<String, String> data = WebUtil.decodeGET(request.getTarget());
			String uuid = data.get("uuid");
			if (uuid != null) {
				Member mem = server.getDatabase().getMembers()
						.getMemberByBadge(uuid);
				if (mem == null) {
					try {
						mem = server.getDatabase().getMembers()
								.getMemberByUUID(Long.valueOf(uuid));
						BufferedImage img = ImageIO.read(new File("data/mugs/"
								+ mem.getIMG()));
						ImageIO.write(img, "PNG", rawBody);
						img.flush();
						img = null;
					} catch (Exception e) {
					}
				} else {
					response.setValue("Content-Type", "text/html");
					body.println("<html><body>");
					body.println("Invalid user!");
					body.println("</body></html>");
				}
			} else {
				response.setValue("Content-Type", "text/html");
				body.println("<html><body>");
				body.println("Requires a start and end key!");
				body.println("</body></html>");
			}

			body.close();
			rawBody.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}