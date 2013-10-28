package com.skunk.clock.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

public class ResourceContainer implements Container {
	@Override
	public void handle(Request req, Response resp) {
		String url = req.getTarget();
		if (url.contains("..")) {
			return;
		}
		File f = new File("./" + req.getTarget());
		if (!f.exists()) {
			return;
		}
		try {
			byte[] buffer = new byte[1024];
			FileInputStream fin = new FileInputStream(f);
			while (true) {
				int read = fin.read(buffer);
				if (read < 0) {
					break;
				}
				resp.getOutputStream().write(buffer, 0, read);
			}
			resp.getOutputStream().close();
			fin.close();
		} catch (IOException e) {
		}
	}
}
