package com.skunk.clock.web.visual;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.simpleframework.http.Request;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberType;
import com.skunk.clock.web.APIHandler;
import com.skunk.clock.web.ClockWebServer;
import com.skunk.clock.web.WebUtil;

public class DatabaseEntryContainer extends APIHandler {
	private static final long DEFAULT_LENGTH = 1000 * 60 * 60 * 24 * 7; // 1
																		// week

	public DatabaseEntryContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request request, PrintStream body) {
		body.println("<html><head>");

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
			body.println("<link rel=\"stylesheet\" href=\"/data/resources/datepickr.css\" type=\"text/css\" media=\"screen\">");
			body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/datepickr.min.js\"></script>");
			body.println("</head><body>");
			body.println("<form method='GET' action=''>");
			body.println("Start: <input type='text' name='start' id='dateEnterA' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("End: <input type='text' name='end' id='dateEnterB' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='checkbox' name='mentor'>Mentors</option><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='checkbox' name='student'>Students</option><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='submit'/></form>");
			body.println("<script type='text/javascript'>new datepickr('dateEnterB', {'dateFormat': 's'});new datepickr('dateEnterA', {'dateFormat': 's'});</script>");
		} else {
			if (start != null && end != null) {
				final long startTime = Long.valueOf(start);
				final long endTime = Long.valueOf(end) < 0 ? Long.MAX_VALUE
						: Long.valueOf(end);
				File[] list = WebUtil.listOrderedDatabase(startTime, endTime);
				final Map<Member, List<Entry<Long, Long>>> totalData = new HashMap<Member, List<Entry<Long, Long>>>();

				MemberType[] types = new MemberType[] { MemberType.STUDENT };
				if (data.containsKey("student") && data.containsKey("mentor")) {
					types = new MemberType[0];
				} else if (data.containsKey("mentor")) {
					types = new MemberType[] { MemberType.MENTOR,
							MemberType.COACH };
				}
				boolean showEmpty = data.containsKey("all");

				for (File f : list) {
					ClocktimeDatabase db = server.getDatabase().getClocktime(
							f.getAbsolutePath());
					List<Entry<Member, Clocktime>> lst = db
							.getListByType(types);
					for (Entry<Member, Clocktime> entry : lst) {
						if (entry.getValue().getClockTime() <= 0 && !showEmpty) {
							continue;
						}
						List<Entry<Long, Long>> supp = totalData.get(entry
								.getKey());
						if (supp == null) {
							supp = new ArrayList<Entry<Long, Long>>();
							totalData.put(entry.getKey(), supp);
						}
						Iterator<Entry<Long, Long>> itr = entry.getValue()
								.iterator();
						while (itr.hasNext()) {
							supp.add(itr.next());
						}
					}
				}
				body.println("<style type='text/css'>\n"
						+ WebUtil.getTableStyle() + "\n</style>");
				body.println("</head><body>");

				body.println("From " + WebUtil.formatDate(startTime) + " to "
						+ WebUtil.formatDate(endTime) + "</br>");

				if (totalData.size() != 0) {
					body.println("<table id='greenTable'>");
					body.println("<tr>");
					body.println("<tr><th>User</th><th>Time IN</th><th>Time OUT</th>");
					body.println("</tr>");
					for (Entry<Member, List<Entry<Long, Long>>> ent : totalData
							.entrySet()) {
						for (Entry<Long, Long> time : ent.getValue()) {
							body.println("<tr><td>" + ent.getKey().getName()
									+ "</td>");
							body.println("<td>"
									+ WebUtil.formatDateTime(time.getKey())
									+ "</td>");
							body.println("<td>"
									+ WebUtil.formatDateTime(time.getValue())
									+ "</td>");
							body.println("</tr>");
						}
					}
					body.println("</table>");
				}
			} else {
				body.println("Requires a start and end key!");
			}
		}

		body.println("</body></html>");
	}
}
