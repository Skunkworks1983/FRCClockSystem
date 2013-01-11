package com.skunk.clock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.skunk.clock.db.Member;
import com.skunk.clock.db.MemberDatabase;

public class CreateMugs {
	private static final int MUG_WIDTH = 54;// 48;
	private static final int MUG_HEIGHT = 64;// 64;
	private static final int NAME_LENGTH = 8;

	public static void main(String[] args) throws IOException {
		MemberDatabase memDB = new MemberDatabase();
		memDB.read();
		for (Member m : memDB) {
			BufferedImage img = create(
					new File("data/mugs_large/" + m.getIMG()), m.getName());
			ImageIO.write(img, "JPG", new File("data/mugs/" + m.getIMG()));
		}
	}

	private static BufferedImage create(File f, String name) {
		Image tmp = null;
		try {
			tmp = ImageIO.read(f);
		} catch (Exception e) {
			System.out.println("Could not read: " + name + "'s image at "
					+ f.getAbsolutePath());
		}
		BufferedImage real = new BufferedImage(MUG_WIDTH, MUG_HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = real.createGraphics();
		if (tmp != null) {
			float aspect = (((float) tmp.getWidth(null)) / ((float) tmp
					.getHeight(null)));
			if (aspect * MUG_HEIGHT > MUG_WIDTH) {
				int height = (int) (((float) MUG_WIDTH) / aspect);
				g.drawImage(tmp, 0, (MUG_HEIGHT / 2) - (height / 2), MUG_WIDTH,
						(MUG_HEIGHT / 2) + (height / 2), 0, 0,
						tmp.getWidth(null), tmp.getHeight(null), null);
			} else {
				int width = (int) (aspect * ((float) MUG_HEIGHT));
				g.drawImage(tmp, (MUG_WIDTH / 2) - (width / 2), 0,
						(MUG_WIDTH / 2) + (width / 2), MUG_HEIGHT, 0, 0,
						tmp.getWidth(null), tmp.getHeight(null), null);
			}
			tmp.flush();
		}
		g.setColor(new Color(0f, 0f, 0f, 0.5f));
		g.fillRect(0, MUG_HEIGHT - g.getFontMetrics().getAscent(), MUG_WIDTH, g
				.getFontMetrics().getAscent());
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.drawString(name.substring(0, Math.min(name.length(), NAME_LENGTH)),
				0, MUG_HEIGHT - 1);
		g.dispose();
		return real;
	}
}
