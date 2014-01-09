package com.skunk.clock;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberType;
import com.skunk.clock.db.MemberDatabase;

public class BarcodeGenerator {
	private static final String path = "http://www.barcodesinc.com/generator/image.php?code=%s&style=196&type=C128B&width=200&height=50&xres=1&font=3";

	public static void main(String[] args) throws IOException {
		MemberDatabase db = new MemberDatabase();
		db.read();
		Iterator<Member> iterator = db.iterator();
		List<Member> list = new ArrayList<Member>(db.size());
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		Collections.sort(list, new Comparator<Member>() {
			@Override
			public int compare(Member o1, Member o2) {
				return o1.getReversedName().compareTo(o2.getReversedName());
			}
		});

		int q = 0;
		int p = 0;
		while (p < list.size()) {
			BufferedImage image = new BufferedImage(4 * 225, 10 * 100,
					BufferedImage.TYPE_BYTE_GRAY);
			Graphics g = image.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, 5 * 255, 10 * 100);
			g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 1.25f));
			for (int i = 0; i < 4 && p < list.size(); i++) {
				for (int j = 0; j < 10 && p < list.size(); j++) {
					Member mem = list.get(p++);
					if (mem.getType() == MemberType.ADMIN
							|| mem.getType() == MemberType.COACH) {
						if (p < list.size()) {
							mem = list.get(p++);
						} else {
							mem = null;
						}
					}
					if (mem != null) {
						int x = i * 225;
						int y = j * 100;
						g.setColor(Color.BLACK);
						Rectangle2D sb = g.getFontMetrics().getStringBounds(
								mem.getName(), g);
						g.drawString(mem.getName(),
								x + 112 - (int) (sb.getWidth() / 2), y + 5
										+ (int) sb.getHeight());
						BufferedImage img = ImageIO.read(new URL(path.replace(
								"%s", mem.getBadge())));
						g.drawImage(img, x, y + 35, null);
						System.out.println("Barcode for: " + mem.getName()
								+ " fetched");
						g.drawRect(x, y, 225, 100);
					}
				}
			}
			g.dispose();
			ImageIO.write(image, "PNG", new File("data/barcodes-" + q + ".png"));
			q++;
		}
	}
}
