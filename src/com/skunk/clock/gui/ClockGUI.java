package com.skunk.clock.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.skunk.clock.Util;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;
import com.skunk.clock.db.MemberDatabase;

public class ClockGUI extends JFrame implements KeyListener {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private MemberDatabase memDB = new MemberDatabase();
	private ClocktimeDatabase clockDB = new ClocktimeDatabase();

	private static final int MUG_WIDTH = 54;// 48;
	private static final int MUG_HEIGHT = 64;// 64;
	private static final int MUG_H_PADDING = 1;
	private static final int MUG_V_PADDING = 1;
	private static final long CLOCKIN_DISPLAY_TIME = 1000;

	private List<Member> clockedIn = new ArrayList<Member>();
	private JTextField enter;
	private JLabel lblEntryError;
	private JScrollBar studentScroll;
	private JScrollBar mentorScroll;
	private JPanel mentorPanel;
	private JPanel studentPanel;
	private JPanel changingPanel;
	private JPanel controlPanel;

	private Object bufferLock = new Object();
	private VolatileImage mentors = null, students = null, changing = null;
	private int[] groupBaseline = new int[MemberGroup.values().length];
	private AtomicBoolean dirty = new AtomicBoolean();

	private Map<Long, Image> images = new HashMap<Long, Image>();

	private Member currentMember;
	private long currentTime;
	private boolean state;

	/**
	 * Create the frame.
	 */
	public ClockGUI() {
		System.out.println("Creating frame...");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, 800, 600);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				dirty.set(true);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				dirty.set(true);
			}

			@Override
			public void windowActivated(WindowEvent e) {
				dirty.set(true);
			}
		});
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		setContentPane(contentPane);
		addKeyListener(this);
		contentPane.addKeyListener(this);

		studentPanel = new JPanel();
		studentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contentPane.add(studentPanel);
		studentPanel.setLayout(new BorderLayout(0, 0));

		studentScroll = new JScrollBar();
		studentPanel.add(studentScroll, BorderLayout.WEST);
		studentPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				checkBuffers();
			}
		});
		studentScroll.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				dirty.set(true);
			}
		});

		controlPanel = new JPanel();
		controlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		contentPane.add(controlPanel);
		controlPanel.setLayout(new BorderLayout(0, 0));

		entryPanel = new JPanel();
		controlPanel.add(entryPanel, BorderLayout.NORTH);

		enter = new JTextField();
		entryPanel.add(enter);
		enter.addKeyListener(this);
		enter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] info = login(enter.getText().trim(), true);
				lblEntryError.setText(info[1]);
				lblEntryError.setToolTipText(info[2]);
				enter.setText("");
			}
		});
		enter.setColumns(10);

		lblEntryError = new JLabel("");
		entryPanel.add(lblEntryError);

		changingPanel = new JPanel();
		controlPanel.add(changingPanel);
		changingPanel.setLayout(null);

		mentorPanel = new JPanel();
		mentorPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		contentPane.add(mentorPanel);
		mentorPanel.setLayout(new BorderLayout(0, 0));

		mentorScroll = new JScrollBar();
		mentorScroll.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				dirty.set(true);
			}
		});
		mentorPanel.add(mentorScroll, BorderLayout.EAST);
		mentorPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				checkBuffers();
			}
		});
		enter.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				enter.requestFocusInWindow();
			}
		});
	}

	public void validate() {
		int cPanelWidth = 100;
		mentorPanel.setPreferredSize(new Dimension(
				(getWidth() - cPanelWidth) / 3, getHeight()));
		controlPanel.setMaximumSize(new Dimension(cPanelWidth, getHeight()));
		studentPanel.setPreferredSize(new Dimension(
				2 * (getWidth() - cPanelWidth) / 3, getHeight()));
		super.validate();
	}

	/**
	 * 
	 * @param s
	 * @return error
	 */
	public String[] login(String s, boolean interact) {
		String[] errors = new String[] { "", "", "" };
		boolean hadBadge = true;
		try {
			Member m = memDB.getMemberByBadge(s);
			if (m == null) {
				m = memDB.getMemberByUUID(Long.valueOf(s));
				hadBadge = false;
			}
			if (m == null) {
				throw new NullPointerException();
			}
			if (clockedIn.contains(m)) {
				clockedIn.remove(m);
				clockDB.getClocktime(m).clockOut(hadBadge);
				if (m.getType() == MemberType.COACH) {
					if (!interact) {
						errors[1] = "Coaches can't sign out through terminal...";
						return errors;
					}
					if (JOptionPane.showConfirmDialog(this,
							"Do you wish to check all members out?",
							"Coach Clocking Out...", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						clockedIn.clear();
						clockDB.clockOutAllWith(60 * 60 * 1000);
						clockedIn.clear();
						try {
							clockDB.save();
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(null, e1.toString());
						}
					}
				}
				if (interact) {
					state = false;
				} else {
					errors[0] = m.toString() + " checked out";
				}
			} else {
				if (m.isInGroup(MemberGroup.LEAD)) {
					clockedIn.add(0, m);
				} else {
					clockedIn.add(m);
				}
				clockDB.getClocktime(m).clockIn(hadBadge);
				if (interact) {
					state = true;
				} else {
					errors[0] = m.toString() + " checked in";
				}
			}
			try {
				loadUserProfile(m);
			} catch (Exception ex) {
				errors[1] = "IO: Unable to read file.";
				errors[2] = "I/O Error!";
			}
			if (interact) {
				currentMember = m;
				currentTime = System.currentTimeMillis();
			}
			checkBuffers();
		} catch (NumberFormatException ex) {
			errors[1] = "NaN: " + s;
			errors[2] = "Not a number!";
		} catch (NullPointerException ex) {
			errors[1] = "NaM: " + s;
			errors[2] = "Not a member!";
		}
		return errors;
	}

	private void loadUserProfile(Member m) throws IOException {
		if (!images.containsKey(m)) {
			images.put(m.getUUID(),
					ImageIO.read(new File("data/mugs/" + m.getIMG())));
		}
	}

	protected void repaintClocked() {
		if (students != null && mentors != null) {
			int mentorID = 0;
			int[] studentID = new int[MemberGroup.values().length];

			Graphics gS = students.createGraphics();
			Graphics gM = mentors.createGraphics();
			gS.clearRect(0, 0, students.getWidth(), students.getHeight());
			gM.clearRect(0, 0, mentors.getWidth(), mentors.getHeight());
			for (Member mem : clockedIn) {
				Image i = images.get(mem.getUUID());
				if (mem.getType() == MemberType.STUDENT) {
					for (MemberGroup group : mem.getGroups()) {
						int panelX = studentID[group.ordinal()]
								% (int) (students.getWidth() / MUG_WIDTH)
								* (MUG_WIDTH + MUG_H_PADDING) + MUG_H_PADDING;
						int panelY = (int) (studentID[group.ordinal()] / (int) (students
								.getWidth() / MUG_WIDTH))
								* (MUG_HEIGHT + MUG_V_PADDING)
								+ groupBaseline[group.ordinal()];
						studentID[group.ordinal()]++;
						if (i != null) {
							gS.drawImage(i, panelX, panelY, panelX + MUG_WIDTH,
									panelY + MUG_HEIGHT, 0, 0, MUG_WIDTH,
									MUG_HEIGHT, null);
						}
					}
				} else {
					int panelX = mentorID
							% (int) (mentors.getWidth() / MUG_WIDTH)
							* (MUG_WIDTH + MUG_H_PADDING) + MUG_H_PADDING;
					int panelY = (int) (mentorID / (int) (mentors.getWidth() / MUG_WIDTH))
							* (MUG_HEIGHT + MUG_V_PADDING);
					mentorID++;
					if (i != null) {
						gM.drawImage(i, panelX, panelY, panelX + MUG_WIDTH,
								panelY + MUG_HEIGHT, 0, 0, MUG_WIDTH,
								MUG_HEIGHT, null);
					}
				}
			}
			gS.setColor(Color.BLACK);
			gS.setFont(gS.getFont().deriveFont(Font.BOLD).deriveFont(16f));
			for (int i = 0; i < groupBaseline.length; i++) {
				if (groupBaseline[i] >= 0) {
					gS.drawString(MemberGroup.values()[i].formattedName(), 0,
							groupBaseline[i] - MUG_V_PADDING);
				}
			}
			gS.dispose();
			gM.dispose();

			gS = studentPanel.getGraphics();
			gS.clearRect(studentScroll.getWidth(), 1, students.getWidth(),
					studentPanel.getHeight() - 2);
			gS.drawImage(students, studentScroll.getWidth(), 1,
					students.getWidth(), studentPanel.getHeight() - 1, 0,
					studentScroll.getValue(), students.getWidth(),
					studentScroll.getValue() + studentPanel.getHeight(), null);
			if (studentScroll.isVisible())
				studentScroll.paint(gS);

			gM = mentorPanel.getGraphics();
			gM.clearRect(1, 1, mentors.getWidth(), mentorPanel.getHeight() - 2);
			gM.drawImage(mentors, 1, 1, mentors.getWidth(),
					mentorPanel.getHeight() - 1, 0, mentorScroll.getValue(),
					mentors.getWidth(),
					mentorScroll.getValue() + mentorPanel.getHeight() - 1, null);
			if (mentorScroll.isVisible())
				mentorScroll.paint(gM.create(mentorScroll.getX(),
						mentorScroll.getY(), mentorScroll.getWidth(),
						mentorScroll.getHeight()));
		}
		if (changing != null) {
			Graphics g = changingPanel.getGraphics();
			Graphics v = changing.getGraphics();
			v.clearRect(0, 0, changing.getWidth(), changing.getHeight());
			if (currentTime >= System.currentTimeMillis()
					- CLOCKIN_DISPLAY_TIME
					&& currentMember != null) {
				loginPainted.set(true);
				Image id = images.get(currentMember.getUUID());
				if (id != null) {
					v.drawImage(id,
							(changing.getWidth() / 2) - (MUG_WIDTH / 2), 0,
							(changing.getWidth() / 2) + (MUG_WIDTH / 2),
							MUG_HEIGHT, 0, 0, MUG_WIDTH, MUG_HEIGHT, null);
				}
				v.setColor(Color.BLACK);

				Rectangle2D strBounds = v.getFontMetrics().getStringBounds(
						currentMember.getName(), v);
				int y = (id != null ? id.getHeight(null) : 0)
						+ (int) strBounds.getHeight();
				v.drawString(currentMember.getName(), (changing.getWidth() / 2)
						- (int) (strBounds.getWidth() / 2), y);

				strBounds = v.getFontMetrics().getStringBounds(
						currentMember.getType().formattedName(), v);
				v.drawString(currentMember.getType().formattedName(),
						(changing.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);

				strBounds = v.getFontMetrics().getStringBounds(
						state ? "Checked In" : "Checked Out", v);
				v.drawString(state ? "Checked In" : "Checked Out",
						(changing.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);

				strBounds = v.getFontMetrics().getStringBounds(
						"Time: "
								+ Util.formatTime(clockDB.getClocktime(
										currentMember).getClockTime()), v);
				v.drawString(
						"Time: "
								+ Util.formatTime(clockDB.getClocktime(
										currentMember).getClockTime()),
						(changing.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);

				for (MemberGroup gr : currentMember.getGroups()) {
					strBounds = v.getFontMetrics().getStringBounds(
							gr.formattedName(), v);
					v.drawString(gr.formattedName(), (changing.getWidth() / 2)
							- (int) (strBounds.getWidth() / 2),
							y += strBounds.getHeight() + 5);
				}
			}
			g.drawImage(changing, 0, 0, null);
			v.dispose();
		}
	}

	AtomicBoolean loginPainted = new AtomicBoolean();

	public void repaintLoop() {
		synchronized (bufferLock) {
			if (dirty.getAndSet(false)) {
				repaintClocked();
			} else if (currentTime < System.currentTimeMillis()
					- CLOCKIN_DISPLAY_TIME - 10) {
				if (loginPainted.getAndSet(false)) {
					repaintClocked();
				}
			}
		}
	}

	private void checkMentorBuffer() {
		int mentorCount = clockDB.getClockedByType(MemberType.MENTOR,
				MemberType.COACH)[0];
		if (mentors != null) {
			mentors.flush();
		}
		int panelWidth = (mentorPanel.getWidth() - mentorScroll.getWidth() - 1)
				/ (MUG_WIDTH + MUG_H_PADDING);
		int panelsHeight = (int) Math.ceil(((float) mentorCount)
				/ ((float) panelWidth));
		mentors = createVolatileImage(
				mentorPanel.getWidth() - mentorScroll.getWidth(), panelsHeight
						* (MUG_HEIGHT + MUG_V_PADDING) + 1);

		// scroll bars
		int scroll = mentors.getHeight() - mentorPanel.getHeight();
		if (scroll <= 0) {
			mentorScroll.setVisible(false);
			mentorScroll.setValue(0);
		} else {
			mentorScroll.setVisible(true);
			if (mentorScroll.getValue() > scroll) {
				mentorScroll.setValue(scroll);
			}
			mentorScroll.setMaximum(scroll + 10);
		}
	}

	private void checkStudentBuffer() {
		int[] studentCount = clockDB.getClockedByType(MemberType.STUDENT);

		if (students != null) {
			students.flush();
		}
		int panelWidth = (studentPanel.getWidth() - studentScroll.getWidth() - 1)
				/ (MUG_WIDTH + MUG_H_PADDING);
		float panelsHeight = 0;
		for (int i = 1; i < studentCount.length; i++) {
			if (studentCount[i] > 0) {
				panelsHeight += 18f / (MUG_HEIGHT + MUG_V_PADDING);
				groupBaseline[i - 1] = (int) (panelsHeight * (float) (MUG_HEIGHT + MUG_V_PADDING));
				panelsHeight += (int) Math.ceil(((float) studentCount[i])
						/ ((float) panelWidth));
			} else {
				groupBaseline[i - 1] = -1;
			}
		}
		students = createVolatileImage(
				studentPanel.getWidth() - studentScroll.getWidth(),
				(int) (panelsHeight * (float) (MUG_HEIGHT + MUG_V_PADDING)) + 1);
		// scroll bars
		int scroll = students.getHeight() - studentPanel.getHeight() + 1;
		if (scroll <= 0) {
			studentScroll.setVisible(false);
			studentScroll.setValue(0);
		} else {
			studentScroll.setVisible(true);
			if (studentScroll.getValue() > scroll) {
				studentScroll.setValue(scroll);
			}
			studentScroll.setMaximum(scroll + 10);
		}
	}

	protected void checkBuffers() {
		synchronized (bufferLock) {
			checkMentorBuffer();
			checkStudentBuffer();
			if (changingPanel.getWidth() > 0
					&& changingPanel.getHeight() > 0
					&& (changing == null || changing.getWidth() != changingPanel
							.getWidth())) {
				if (changing != null) {
					changing.flush();
				}
				changing = createVolatileImage(changingPanel.getWidth(),
						changingPanel.getHeight());
			}
			dirty.set(true);
		}
	}

	public void loadDB() {
		synchronized (bufferLock) {
			System.out.println("Loading db...");
			try {
				memDB.read();
			} catch (Exception e) {
			}
			System.out.println("Preallocating clock db...");
			for (Member m : memDB) {
				clockDB.getClocktime(m);
			}
			System.out.println("Loading user images...");
			int i = 0;
			for (Member m : memDB) {
				try {
					loadUserProfile(m);
					System.out.println("Loaded image for " + m.getName()
							+ " -> " + m.getIMG() + "\t(" + (i++) + "/"
							+ memDB.size());
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	long lastFS = 0;
	private JPanel entryPanel;

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F11) {
			if (lastFS > System.currentTimeMillis() - 500) {
				return;
			}
			lastFS = System.currentTimeMillis();
			// TODO Fullscreen?
			e.consume();
		}
	}

	public MemberDatabase getMembers() {
		return memDB;
	}

	public ClocktimeDatabase getClock() {
		return clockDB;
	}
}
