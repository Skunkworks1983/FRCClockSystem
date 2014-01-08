package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.simpleframework.http.Request;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberType;

public class RawPeriodContainer extends APIHandler {
	private static final long DEFAULT_LENGTH = 1000 * 60 * 60 * 24 * 7; // 1
																		// week

	public RawPeriodContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request request, PrintStream body) {
		Map<String, String> data = WebUtil.decodeGET(request.getTarget());
		String start = data.get("start");
		String end = data.get("end");
		boolean flaggy = false;
		if (start == null && end != null) {
			start = String.valueOf(Long.valueOf(end) - DEFAULT_LENGTH);
		} else if (start != null && end == null) {
			end = String.valueOf(Long.valueOf(start) + DEFAULT_LENGTH);
		} else if (start == null && end == null) {
			Calendar today = Calendar.getInstance();
			today.set(Calendar.getInstance().get(Calendar.YEAR), Calendar
					.getInstance().get(Calendar.MONTH), Calendar.getInstance()
					.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
			start = String.valueOf(today.getTimeInMillis() - DEFAULT_LENGTH);
			end = String.valueOf(today.getTimeInMillis()
					+ (24 * 60 * 60 * 1000) - 1);
			flaggy = true;
		}
		if (flaggy) {
			body.println("<html><head>");
			body.println("<link rel=\"stylesheet\" href=\"/data/resources/datepickr.css\" type=\"text/css\" media=\"screen\">");
			body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/datepickr.min.js\"></script>");
			body.println("</head><body>");
			body.println("<form method='GET' action=''>");
			body.println("Start: <input type='text' name='start' id='dateEnterA' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("End: <input type='text' name='end' id='dateEnterB' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='checkbox' name='mentor'>Mentors</option><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='checkbox' name='student'>Students</option><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='checkbox' name='parts'>Show Days</option><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='submit'/></form>");
			body.println("<script type='text/javascript'>new datepickr('dateEnterB', {'dateFormat': 's'});new datepickr('dateEnterA', {'dateFormat': 's'});</script>");
			body.println("</body></html>");
		} else {
			if (start != null && end != null) {
				final long startTime = Long.valueOf(start);
				final long endTime = Long.valueOf(end) < 0 ? Long.MAX_VALUE
						: Long.valueOf(end);
				File[] list = WebUtil.listOrderedDatabase(startTime, endTime);
				TreeMap<Member, Clocktime> totalData = new TreeMap<Member, Clocktime>(
						new Comparator<Member>() {
							@Override
							public int compare(Member o1, Member o2) {
								return o1.getReversedName()
										.compareToIgnoreCase(
												o2.getReversedName());
							}
						});
				TreeMap<Long, ClocktimeDatabase> dayData = new TreeMap<Long, ClocktimeDatabase>();

				MemberType[] types = new MemberType[] { MemberType.STUDENT };
				if (data.containsKey("student") && data.containsKey("mentor")) {
					types = new MemberType[0];
				} else if (data.containsKey("mentor")) {
					types = new MemberType[] { MemberType.MENTOR,
							MemberType.COACH };
				}
				boolean showEmpty = data.containsKey("all");
				boolean showParts = data.containsKey("parts");
				long totalTime = 0;

				for (File f : list) {
					ClocktimeDatabase db = server.getDatabase().getClocktime(
							f.getAbsolutePath());
					List<Entry<Member, Clocktime>> lst = db
							.getListByType(types);
					if (showParts) {
						dayData.put(db.getCreation(), db);
					}
					totalTime += db.getRequiredTime();
					for (Entry<Member, Clocktime> entry : lst) {
						if (entry.getValue().getClockTime() <= 0 && !showEmpty) {
							continue;
						}
						Clocktime supp = totalData.get(entry.getKey());
						if (supp == null) {
							supp = new Clocktime();
							totalData.put(entry.getKey(), supp);
						}
						supp.insertClockTime(entry.getValue());
					}
				}
				if (totalData.size() != 0) {
					body.print("User,Total Time,Total %");
					if (showParts) {
						for (Long db : dayData.keySet()) {
							body.print("," + WebUtil.formatDate(db));
						}
					}
					body.println();
					for (Entry<Member, Clocktime> entry : totalData.entrySet()) {
						body.print(entry.getKey().getName()
								+ ","
								+ entry.getValue().getClockTime()
								+ ","
								+ WebUtil.formatPercentage(entry.getValue()
										.getClockTime(), totalTime));
						if (showParts) {
							for (ClocktimeDatabase db : dayData.values()) {
								Clocktime c = db.getClocktime(entry.getKey());
								body.print(","
										+ (c != null ? c.getClockTime() : 0));
							}
						}
						body.println();
					}
				}
			} else {
				body.println("Requires a start and end key!");
			}
		}
	}
}
