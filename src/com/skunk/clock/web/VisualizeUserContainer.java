package com.skunk.clock.web;

import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Map;

import org.simpleframework.http.Request;

import com.skunk.clock.db.Clocktime;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberGroup;

public class VisualizeUserContainer extends APIHandler {
	private static final long DEFAULT_LENGTH = 1000 * 60 * 60 * 24 * 7; // 1
																		// week

	public VisualizeUserContainer(ClockWebServer clockWebServer) {
		super(clockWebServer);
	}

	@Override
	public void appendContent(Request request, PrintStream body) {

		Map<String, String> data = WebUtil.decodeGET(request.getTarget());
		String start = data.get("start");
		String end = data.get("end");
		String uuid = data.get("uuid");
		if (uuid == null) {
			body.println("<html><head>");
			body.println("<link rel=\"stylesheet\" href=\"/data/resources/datepickr.css\" type=\"text/css\" media=\"screen\">");
			body.println("<meta charset=\"utf-8\"><script src=\"/data/resources/datepickr.min.js\"></script>");
			body.println("</head><body>");
			body.println("<form method='GET' action=''>");
			body.println("User: <input type='text' name='uuid'/></br>");
			body.println("Start: <input type='text' name='start' id='dateEnterA' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("End: <input type='text' name='end' id='dateEnterB' placeholder='Click to enter date.'/><span style='padding: 0px 25px;'>|</span>");
			body.println("<input type='submit'/></form>");
			body.println("<script type='text/javascript'>new datepickr('dateEnterB', {'dateFormat': 's'});new datepickr('dateEnterA', {'dateFormat': 's'});</script>");
		} else {
			body.println("<html><body>");

			if (start == null && end != null) {
				start = String.valueOf(Long.valueOf(end) - DEFAULT_LENGTH);
			} else if (start != null && end == null) {
				end = String.valueOf(Long.valueOf(start) + DEFAULT_LENGTH);
			} else if (start == null && end == null) {
				Calendar today = Calendar.getInstance();
				today.set(Calendar.getInstance().get(Calendar.YEAR), Calendar
						.getInstance().get(Calendar.MONTH), Calendar
						.getInstance().get(Calendar.DAY_OF_MONTH), 0, 0, 0);
				start = String
						.valueOf(today.getTimeInMillis() - DEFAULT_LENGTH);
				end = String.valueOf(today.getTimeInMillis()
						+ (24 * 60 * 60 * 1000) - 1);
			}
			if (start != null && end != null && uuid != null) {
				final long startTime = Long.valueOf(start);
				final long endTime = Long.valueOf(end) < 0 ? Long.MAX_VALUE
						: Long.valueOf(end);
				Member mem = server.getDatabase().getMembers()
						.getMemberByBadge(uuid);
				if (mem == null) {
					try {
						mem = server.getDatabase().getMembers()
								.getMemberByUUID(Long.valueOf(uuid));
					} catch (Exception e) {
					}
				}
				if (mem != null) {
					File[] list = WebUtil.listOrderedDatabase(startTime,
							endTime);
					long[][] timeHeaders = new long[list.length][2];
					ClocktimeDatabase[] mapping = new ClocktimeDatabase[list.length];
					int i = 0;
					for (File f : list) {
						ClocktimeDatabase db = server.getDatabase()
								.getClocktime(f.getAbsolutePath());
						Clocktime info = db.getClocktime(mem);
						if (info != null) {
							timeHeaders[i][0] = db.getCreation();
							timeHeaders[i][1] = info.getClockTime();
							mapping[i] = db;
							i++;
						} else {
							timeHeaders[i][0] = db.getCreation();
							timeHeaders[i][1] = 0;
							mapping[i] = db;
							i++;
						}
					}

					body.println("<div style='float: left;'>");
					body.println("<img src='/visual/user/image?uuid=" + uuid
							+ "'/>");
					body.println("</div>");

					body.println("<b>" + mem.getName() + "</b></br>");
					if (mem.getGroups().length > 0
							&& mem.getGroups()[0] != MemberGroup.UNDEFINED) {
						body.print("In groups: ");
						for (int j = 0; j < mem.getGroups().length; j++) {
							if (j == mem.getGroups().length - 1) {
								if (mem.getGroups().length == 2) {
									body.print(" and ");
								} else if (mem.getGroups().length > 2) {
									body.print(", and ");
								}
							} else if (j > 0) {
								body.print(", ");
							}
							body.print(mem.getGroups()[j].formattedName());
						}
						body.println("<br>");
					}
					body.println("Type: " + mem.getType().formattedName()
							+ "</br>");

					long totalTime = 0;
					long requiredTotal = 0;
					for (int j = 0; j < i; j++) {
						requiredTotal += mapping[j].getRequiredTime();
						totalTime += timeHeaders[j][1];
					}
					body.println("Graph User Time: <a href='/graph/user?start="
							+ startTime + "&end=" + endTime + "&uuid="
							+ mem.getUUID() + "'>Here</a></br>");
					body.println("Total time: "
							+ WebUtil.formatTimeLong(totalTime) + "</br>");
					body.println("Percentage: "
							+ WebUtil
									.formatPercentage(totalTime, requiredTotal)
							+ "</br>");

					body.println("<table border=1>");
					body.println("<tr><th>Day</th><th>Time</th><th>Percentage</th></tr>");
					for (int j = 0; j < i; j++) {
						body.println("<tr><td>"
								+ WebUtil.formatDate(timeHeaders[j][0])
								+ (mapping[j].getTag() != null ? " "
										+ mapping[j].getTag() : "")
								+ "</td><td>"
								+ WebUtil.formatTimeLong(timeHeaders[j][1])
								+ "</td><td>"
								+ WebUtil.formatPercentage(timeHeaders[j][1],
										mapping[j].getRequiredTime())
								+ "</td></tr>");
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
