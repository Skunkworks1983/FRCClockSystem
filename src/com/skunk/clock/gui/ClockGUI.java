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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.skunk.clock.Configuration;
import com.skunk.clock.CreateMugs;
import com.skunk.clock.Util;
import com.skunk.clock.db.ClocktimeDatabase;
import com.skunk.clock.db.Member;
import com.skunk.clock.db.Member.MemberGroup;
import com.skunk.clock.db.Member.MemberType;
import com.skunk.clock.db.MemberDatabase;

public class ClockGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	/**
	 * The member database.
	 */
	private MemberDatabase memDB = new MemberDatabase();
	/**
	 * The clock database.
	 */
	private ClocktimeDatabase clockDB = new ClocktimeDatabase();

	// Components
	private JPanel contentPane;
	private JTextField uuidEntryField;
	private JLabel lblEntryError;
	private JScrollBar studentScroll, mentorScroll;
	private JPanel mentorPanel, studentPanel, changingPanel, entryPanel,
			controlPanel;

	private BufferedImage currentScreensaver;
	private BufferedImage nextScreensaver;
	private Thread nextScreensaverLoader;

	/**
	 * A cached list of all clocked-in members, to speed up the rendering
	 * process.
	 */
	private List<Member> clockedIn = new ArrayList<Member>();
	/**
	 * Object lock that prevents the modification of buffers during render.
	 */
	private Object bufferLock = new Object();
	/**
	 * Mentors, students, and information display buffers.
	 */
	private VolatileImage mentorBuffer = null, studentBuffer = null,
			infoBuffer = null;
	/**
	 * The y-offset for every member group in the student panel.
	 */
	private int[] groupBaseline = new int[MemberGroup.values().length];
	/**
	 * If all the panels need to be repainted.
	 */
	private AtomicBoolean dirty = new AtomicBoolean();

	/**
	 * Pre-loaded map of loaded profile images to UUIDs.
	 */
	private Map<Long, Image> images = new HashMap<Long, Image>();

	/**
	 * The last member to clock in or out.
	 */
	private Member lastMember;
	/**
	 * The time at which the last member clocked in or out. This allows it to
	 * stop showing after a certain amount of time.
	 */
	private long lastMemberClockedTime;
	/**
	 * The clock in/out state of the currently displaying member.
	 */
	private boolean lastMemberClockState;
	/**
	 * The last time there was an action on the frame.
	 */
	private long lastFrameAction = System.currentTimeMillis();
	/**
	 * The last time the screensaver frame was updated.
	 */
	private long lastScreensaverFrame = 0;
	/**
	 * If the frame currently shows the screensaver.
	 */
	private AtomicBoolean isScreensaver = new AtomicBoolean(false);

	private File[] screensaverFiles;

	/**
	 * Creates the Clock GUI, creates the components, adds them, and registers
	 * the listeners.
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

		screensaverFiles = new File("data/screensaver")
				.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						String name = file.getName();
						return file.isFile()
								&& (name.endsWith(".jpeg")
										|| name.endsWith(".png")
										|| name.endsWith(".jpg") || name
											.endsWith(".gif"));
					}
				});
		if (screensaverFiles == null) {
			screensaverFiles = new File[0];
		}

		// Create the content pane.
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		setContentPane(contentPane);

		// Create the students panel, and registers
		// a listener to cause the panel to repaint on change.
		studentPanel = new JPanel();
		studentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		studentPanel.setLayout(new BorderLayout(0, 0));
		studentPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				checkBuffers();
			}
		});
		contentPane.add(studentPanel);

		// Creates the students scroll bar, and registers a listener to cause
		// the panel to repaint on change.
		studentScroll = new JScrollBar();
		studentPanel.add(studentScroll, BorderLayout.WEST);
		studentScroll.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				dirty.set(true);
			}
		});

		// Creates the central control panel.
		controlPanel = new JPanel();
		controlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		contentPane.add(controlPanel);
		controlPanel.setLayout(new BorderLayout(0, 0));

		// Creates the entry panel.
		entryPanel = new JPanel();
		controlPanel.add(entryPanel, BorderLayout.NORTH);

		// Creates the entry field for student UUIDs, adds a listener to process
		// entry events, and also one to ensure it doens't lose focus.
		uuidEntryField = new JTextField();

		entryPanel.add(uuidEntryField);
		uuidEntryField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] info = clockInOut(uuidEntryField.getText().trim(),
						true);
				lblEntryError.setText(info[1]);
				lblEntryError.setToolTipText(info[2]);
				uuidEntryField.setText("");
			}
		});
		uuidEntryField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				lastFrameAction = System.currentTimeMillis();
			}
		});
		uuidEntryField.setColumns(10);
		uuidEntryField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				uuidEntryField.requestFocusInWindow();
			}
		});

		// Creates the entry error label.
		lblEntryError = new JLabel("");
		entryPanel.add(lblEntryError);

		// Creates the user information panel.
		changingPanel = new JPanel();
		controlPanel.add(changingPanel);
		changingPanel.setLayout(null);

		// Create the students panel, and registers
		// a listener to cause the panel to repaint on change.
		mentorPanel = new JPanel();
		mentorPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		contentPane.add(mentorPanel);
		mentorPanel.setLayout(new BorderLayout(0, 0));
		mentorPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				checkBuffers();
			}
		});

		// Creates the mentors scroll bar, and registers a listener to cause
		// the panel to repaint on change.
		mentorScroll = new JScrollBar();
		mentorScroll.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				dirty.set(true);
			}
		});
		mentorPanel.add(mentorScroll, BorderLayout.EAST);

		tryLoadNextScreensaver();

		lastFrameAction = System.currentTimeMillis();
		repaint();
	}

	/**
	 * Sets the preferred size of components, then runs the frame's validate
	 * method.
	 */
	@Override
	public void validate() {
		mentorPanel.setPreferredSize(new Dimension(
				(getWidth() - Configuration.CENTER_WIDTH) / 3, getHeight()));
		controlPanel.setMaximumSize(new Dimension(Configuration.CENTER_WIDTH,
				getHeight()));
		studentPanel
				.setPreferredSize(new Dimension(
						2 * (getWidth() - Configuration.CENTER_WIDTH) / 3,
						getHeight()));
		super.validate();
	}

	@Override
	public void repaint() {
		super.repaint();
		dirty.set(true);
	}

	public void checkCurrentDatabase() {
		if (System.currentTimeMillis() - clockDB.getCreation() > Configuration.CACHE_EXIPRY_TIME) {
			System.out.println("DUMPING DATABASE!!!!");
			// output the database
			clockDB.clockOutAllWith(60 * 60 * 1000);
			try {
				String date = Util.formatDate(new Date(clockDB.getCreation()));
				File clocks = new File("data/time_chunk_" + date + ".csv");
				clockDB.cachedSave(clocks);
				try {
					FileInputStream input = new FileInputStream(clocks);
					FileOutputStream output = new FileOutputStream(new File(
							clocks.getAbsolutePath().concat(".backup")));
					byte[] buffer = new byte[1024];
					int count;
					while ((count = input.read(buffer)) > 0) {
						output.write(buffer, 0, count);
					}
					try {
						output.getFD().sync();
					} catch (SyncFailedException e) {
						System.out.println(e.getMessage());
					}
					output.close();
					input.close();
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			clockDB = new ClocktimeDatabase();
		}
	}

	/**
	 * Clocks the user specified, with optional user interaction.
	 * 
	 * @param user
	 *            the user to clock in/out
	 * @param interact
	 *            if the user should be displayed on the central panel
	 * @return the error messages, in the format {Success Message, Short Error,
	 *         Extended Error}
	 */
	public String[] clockInOut(String user, boolean interact) {
		checkCurrentDatabase();
		String[] errors = new String[] { "", "", "" };
		boolean hadBadge = true;
		try {
			Member m = memDB.getMemberByBadge(user);
			if (m == null) {
				m = memDB.getMemberByUUID(Long.valueOf(user));
				hadBadge = false;
			}
			if (m == null) {
				throw new NullPointerException();
			}
			if (m.getType() == MemberType.ADMIN) {
				if (!interact) {
					errors[1] = "Admins can't manipulate times through the terminal.";
					return errors;
				}
				uuidEntryField.setText("OVERRIDE");
				String modUserID = JOptionPane
						.showInputDialog("Enter the user to modify.");
				Member modUser = memDB.getMemberByBadge(user);
				if (modUser == null) {
					try {
						modUser = memDB
								.getMemberByUUID(Long.valueOf(modUserID));
					} catch (Exception e) {
					}
				}
				if (modUser != null) {
					String offset = JOptionPane
							.showInputDialog("Enter the time offset in hours.");
					try {
						float hours = Float.valueOf(offset);
						clockDB.getClocktime(modUser).adminClockIn(
								System.currentTimeMillis()
										- (long) (hours * 60f * 60f * 1000f));
						if (!clockedIn.contains(modUser)) {

						}
						if (modUser.isInGroup(MemberGroup.LEAD)) {
							clockedIn.add(0, modUser);
						} else if (modUser.isInGroup(MemberGroup.SUBLEAD)) {
							// Find the last lead; after that
							int i = 0;
							for (i = 0; i < clockedIn.size(); i++) {
								if (!clockedIn.get(i).isInGroup(
										MemberGroup.LEAD)) {
									break;
								}
							}
							clockedIn.add(i, modUser);
						} else {
							clockedIn.add(modUser);
						}
						if (interact) {
							lastMember = modUser;
							lastMemberClockedTime = System.currentTimeMillis()
									- (long) (hours * 24f * 60f * 60f * 1000f);
						}
					} catch (Exception e) {
					}
				}
			} else {
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
								"Coach Clocking Out...",
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							clockedIn.clear();
							clockDB.clockOutAllWith(60 * 60 * 1000);
							clockedIn.clear();
							try {
								clockDB.save();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(null,
										e1.toString());
							}
						}
					}
					if (interact) {
						lastMemberClockState = false;
					} else {
						errors[0] = m.toString() + " checked out";
					}
				} else {
					if (m.isInGroup(MemberGroup.LEAD)) {
						clockedIn.add(0, m);
					} else if (m.isInGroup(MemberGroup.SUBLEAD)) {
						// Find the last lead; after that
						int i = 0;
						for (i = 0; i < clockedIn.size(); i++) {
							if (!clockedIn.get(i).isInGroup(MemberGroup.LEAD)) {
								break;
							}
						}
						clockedIn.add(i, m);
					} else {
						clockedIn.add(m);
					}
					clockDB.getClocktime(m).clockIn(hadBadge);
					if (interact) {
						lastMemberClockState = true;
					} else {
						errors[0] = m.toString() + " checked in";
					}
				}
				if (interact) {
					lastMember = m;
					lastMemberClockedTime = System.currentTimeMillis();
				}
			}
			checkBuffers();
			try {
				String date = Util.formatDate(new Date(clockDB.getCreation()));
				File clocks = new File("data/time_chunk_" + date + ".csv");
				clockDB.cachedSave(clocks);
			} catch (IOException e) {
			}
		} catch (NumberFormatException ex) {
			errors[1] = "NaN: " + user;
			errors[2] = "Not a number!";
		} catch (NullPointerException ex) {
			errors[1] = "NaM: " + user;
			errors[2] = "Not a member!";
		}
		uuidEntryField.requestFocus();
		return errors;
	}

	/**
	 * Repaints the currently clocked in students and mentors panels.
	 */
	private void repaintClocked() {
		if (studentBuffer != null && mentorBuffer != null) {
			int mentorID = 0;
			int[] studentID = new int[MemberGroup.values().length];

			Graphics gS = studentBuffer.createGraphics();
			Graphics gM = mentorBuffer.createGraphics();
			gS.clearRect(0, 0, studentBuffer.getWidth(),
					studentBuffer.getHeight());
			gM.clearRect(0, 0, mentorBuffer.getWidth(),
					mentorBuffer.getHeight());
			for (Member mem : clockedIn) {
				Image i = images.get(mem.getUUID());
				if (mem.getType() == MemberType.STUDENT) {
					for (MemberGroup group : mem.getDisplayedGroups()) {
						int panelX = studentID[group.ordinal()]
								% (int) (studentBuffer.getWidth() / Configuration.MUG_WIDTH)
								* (Configuration.MUG_WIDTH + Configuration.MUG_H_PADDING)
								+ Configuration.MUG_H_PADDING;
						int panelY = (int) (studentID[group.ordinal()] / (int) (studentBuffer
								.getWidth() / Configuration.MUG_WIDTH))
								* (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING)
								+ groupBaseline[group.ordinal()];
						studentID[group.ordinal()]++;
						if (i != null) {
							gS.drawImage(i, panelX, panelY, panelX
									+ Configuration.MUG_WIDTH, panelY
									+ Configuration.MUG_HEIGHT, 0, 0,
									Configuration.MUG_WIDTH,
									Configuration.MUG_HEIGHT, null);
						}
					}
				} else {
					int panelX = mentorID
							% (int) (mentorBuffer.getWidth() / Configuration.MUG_WIDTH)
							* (Configuration.MUG_WIDTH + Configuration.MUG_H_PADDING)
							+ Configuration.MUG_H_PADDING;
					int panelY = (int) (mentorID / (int) (mentorBuffer
							.getWidth() / Configuration.MUG_WIDTH))
							* (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING);
					mentorID++;
					if (i != null) {
						gM.drawImage(i, panelX, panelY, panelX
								+ Configuration.MUG_WIDTH, panelY
								+ Configuration.MUG_HEIGHT, 0, 0,
								Configuration.MUG_WIDTH,
								Configuration.MUG_HEIGHT, null);
					}
				}
			}
			gS.setColor(Color.BLACK);
			gS.setFont(gS.getFont().deriveFont(Font.BOLD).deriveFont(16f));
			for (int i = 0; i < groupBaseline.length; i++) {
				if (groupBaseline[i] >= 0) {
					gS.drawString(MemberGroup.values()[i].formattedName(), 0,
							groupBaseline[i] - Configuration.MUG_V_PADDING);
				}
			}
			gS.dispose();
			gM.dispose();

			gS = studentPanel.getGraphics();
			gS.clearRect(studentScroll.getWidth(), 1, studentBuffer.getWidth(),
					studentPanel.getHeight() - 2);
			gS.drawImage(studentBuffer, studentScroll.getWidth(), 1,
					studentBuffer.getWidth(), studentPanel.getHeight() - 1, 0,
					studentScroll.getValue(), studentBuffer.getWidth(),
					studentScroll.getValue() + studentPanel.getHeight(), null);
			if (studentScroll.isVisible())
				studentScroll.paint(gS);

			gM = mentorPanel.getGraphics();
			gM.clearRect(1, 1, mentorBuffer.getWidth(),
					mentorPanel.getHeight() - 2);
			gM.drawImage(mentorBuffer, 1, 1, mentorBuffer.getWidth(),
					mentorPanel.getHeight() - 1, 0, mentorScroll.getValue(),
					mentorBuffer.getWidth(), mentorScroll.getValue()
							+ mentorPanel.getHeight() - 1, null);
			if (mentorScroll.isVisible())
				mentorScroll.paint(gM.create(mentorScroll.getX(),
						mentorScroll.getY(), mentorScroll.getWidth(),
						mentorScroll.getHeight()));
		}
	}

	/**
	 * Repaints the center control panel's changing state and clock.
	 */
	private void repaintControlPanel() {
		if (infoBuffer != null) {
			Graphics g = changingPanel.getGraphics();
			Graphics v = infoBuffer.getGraphics();
			v.clearRect(0, 0, infoBuffer.getWidth(), infoBuffer.getHeight());
			if (lastMemberClockedTime >= System.currentTimeMillis()
					- Configuration.CLOCKIN_DISPLAY_TIME
					&& lastMember != null) {
				Image id = images.get(lastMember.getUUID());
				if (id != null) {
					v.drawImage(id, (infoBuffer.getWidth() / 2)
							- (Configuration.MUG_CENTRAL_WIDTH / 2), 0,
							(infoBuffer.getWidth() / 2)
									+ (Configuration.MUG_CENTRAL_WIDTH / 2),
							Configuration.MUG_CENTRAL_HEIGHT, 0, 0,
							Configuration.MUG_WIDTH, Configuration.MUG_HEIGHT,
							null);
				}
				v.setColor(Color.BLACK);

				Rectangle2D strBounds = v.getFontMetrics().getStringBounds(
						lastMember.getName(), v);
				int y = (id != null ? Configuration.MUG_CENTRAL_HEIGHT : 0)
						+ (int) strBounds.getHeight();
				v.drawString(lastMember.getName(), (infoBuffer.getWidth() / 2)
						- (int) (strBounds.getWidth() / 2), y);

				strBounds = v.getFontMetrics().getStringBounds(
						lastMember.getType().formattedName(), v);
				v.drawString(
						lastMember.getType().formattedName(),
						(infoBuffer.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);

				strBounds = v.getFontMetrics().getStringBounds(
						"Time: "
								+ Util.formatTime(clockDB.getClocktime(
										lastMember).getClockTime()), v);
				v.drawString(
						"Time: "
								+ Util.formatTime(clockDB.getClocktime(
										lastMember).getClockTime()),
						(infoBuffer.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);

				for (MemberGroup gr : lastMember.getGroups()) {
					strBounds = v.getFontMetrics().getStringBounds(
							gr.formattedName(), v);
					v.drawString(
							gr.formattedName(),
							(infoBuffer.getWidth() / 2)
									- (int) (strBounds.getWidth() / 2),
							y += strBounds.getHeight() + 5);
				}

				Font f = v.getFont();
				v.setFont(v.getFont().deriveFont(Font.BOLD).deriveFont(24f));
				v.setColor(lastMemberClockState ? Color.GREEN : Color.RED);
				strBounds = v.getFontMetrics().getStringBounds(
						lastMemberClockState ? "In" : "Out", v);
				v.drawString(
						lastMemberClockState ? "In" : "Out",
						(infoBuffer.getWidth() / 2)
								- (int) (strBounds.getWidth() / 2),
						y += strBounds.getHeight() + 5);
				v.setFont(f);
			}
			v.setColor(Color.BLACK);
			Font f = v.getFont();
			v.setFont(v.getFont().deriveFont(Font.BOLD).deriveFont(20f));
			String date = DateFormat.getTimeInstance().format(new Date());
			Rectangle2D strBounds = v.getFontMetrics().getStringBounds(date, v);
			v.drawString(date,
					infoBuffer.getWidth() / 2
							- (int) (strBounds.getWidth() / 2),
					infoBuffer.getHeight() - 10);
			g.drawImage(infoBuffer, 0, 0, null);
			v.setFont(f);
			v.dispose();
		}
	}

	/**
	 * Creates a thread to load the next screensaver image, if one doesn't
	 * exist.
	 */
	private void tryLoadNextScreensaver() {
		if (screensaverFiles.length > 0
				&& (nextScreensaverLoader == null || !nextScreensaverLoader
						.isAlive())) {
			nextScreensaverLoader = new Thread(new Runnable() {
				public void run() {
					try {
						File f = screensaverFiles[(int) (Math.random() * screensaverFiles.length)];
						BufferedImage img = ImageIO.read(f);
						System.out.println("SS: " + f.getName());
						nextScreensaver = img;
						if (currentScreensaver == null) {
							currentScreensaver = nextScreensaver;
							nextScreensaver = null;
						}
					} catch (IOException e) {
					}
				}
			});
			nextScreensaverLoader.start();
		}
	}

	/**
	 * Call this method quickly, at least once a second, to actually paint the
	 * queued repaints.
	 */
	public void repaintLoop() {
		synchronized (bufferLock) {
			if (dirty.getAndSet(false)) {
				repaintClocked();
				repaintControlPanel();
				uuidEntryField.getCaret().setVisible(true);
			} else if (lastFrameAction + Configuration.SCREENSAVER_START_TIME < System
					.currentTimeMillis() && screensaverFiles.length > 0) {
				if (uuidEntryField.getCaret().isVisible()) {
					// We have a caret. Hide it and wait awhile.
					uuidEntryField.getCaret().setVisible(false);
					try {
						Thread.sleep(10L);
					} catch (InterruptedException e) {
					}
				}
				isScreensaver.set(true);
				if (lastScreensaverFrame + Configuration.SCREENSAVER_IMG_TIME < System
						.currentTimeMillis()) {
					lastScreensaverFrame = System.currentTimeMillis();
					if (currentScreensaver != null) {
						// getGraphics().drawImage(currentScreensaver, 0, 0,
						// getWidth(), getHeight(), null);
						getGraphics().clearRect(0, 0, getWidth(), getHeight());
						float aspect = (((float) currentScreensaver
								.getWidth(null)) / ((float) currentScreensaver
								.getHeight(null)));
						if (aspect * getHeight() > getWidth()) {
							int height = (int) (((float) getWidth()) / aspect);
							getGraphics().drawImage(currentScreensaver, 0,
									(getHeight() / 2) - (height / 2),
									getWidth(),
									(getHeight() / 2) + (height / 2), 0, 0,
									currentScreensaver.getWidth(null),
									currentScreensaver.getHeight(null), null);
						} else {
							int width = (int) (aspect * ((float) getHeight()));
							getGraphics().drawImage(currentScreensaver,
									(getWidth() / 2) - (width / 2), 0,
									(getWidth() / 2) + (width / 2),
									getHeight(), 0, 0,
									currentScreensaver.getWidth(null),
									currentScreensaver.getHeight(null), null);
						}
					}
					currentScreensaver = nextScreensaver;
					nextScreensaver = null;
					tryLoadNextScreensaver();
				}
				return;
			} else if (lastMemberClockedTime < System.currentTimeMillis()
					- Configuration.CLOCKIN_DISPLAY_TIME - 10
					|| lastMemberClockedTime < System.currentTimeMillis()
							- Configuration.CLOCK_UPDATE_TIME) {
				repaintControlPanel();
			}
			if (isScreensaver.getAndSet(false)) {
				getGraphics().clearRect(0, 0, getWidth(), getHeight());
				repaint();
			}
		}
	}

	/**
	 * Update the size of the mentor's image buffer.
	 */
	private void checkMentorBuffer() {
		int mentorCount = clockDB.getClockedByType(MemberType.MENTOR,
				MemberType.COACH)[0];
		if (mentorBuffer != null) {
			mentorBuffer.flush();
		}
		int panelWidth = (mentorPanel.getWidth() - mentorScroll.getWidth() - 1)
				/ (Configuration.MUG_WIDTH + Configuration.MUG_H_PADDING);
		int panelsHeight = (int) Math.ceil(((float) mentorCount)
				/ ((float) panelWidth));
		mentorBuffer = createVolatileImage(mentorPanel.getWidth()
				- mentorScroll.getWidth(), panelsHeight
				* (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING) + 1);

		// Scroll bars in the student display area.
		int scroll = mentorBuffer.getHeight() - mentorPanel.getHeight();
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

	/**
	 * Update the size of the student's image buffer.
	 */
	private void checkStudentBuffer() {
		int[] studentCount = clockDB.getClockedByType(MemberType.STUDENT);

		if (studentBuffer != null) {
			studentBuffer.flush();
		}
		int panelWidth = (studentPanel.getWidth() - studentScroll.getWidth() - 1)
				/ (Configuration.MUG_WIDTH + Configuration.MUG_H_PADDING);
		float panelsHeight = 0;
		for (int i = 1; i < studentCount.length; i++) {
			if (studentCount[i] > 0) {
				panelsHeight += 18f / (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING);
				groupBaseline[i - 1] = (int) (panelsHeight * (float) (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING));
				panelsHeight += (int) Math.ceil(((float) studentCount[i])
						/ ((float) panelWidth));
			} else {
				groupBaseline[i - 1] = -1;
			}
		}
		studentBuffer = createVolatileImage(
				studentPanel.getWidth() - studentScroll.getWidth(),
				(int) (panelsHeight * (float) (Configuration.MUG_HEIGHT + Configuration.MUG_V_PADDING)) + 1);
		// scroll bars
		int scroll = studentBuffer.getHeight() - studentPanel.getHeight() + 1;
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

	/**
	 * Checks the sizes of both the student, mentor, and information panel
	 * buffers, resizing or creating them as needed. This method also makes the
	 * current window dirty, causing it to be repainted.
	 */
	protected void checkBuffers() {
		synchronized (bufferLock) {
			checkMentorBuffer();
			checkStudentBuffer();
			if (changingPanel.getWidth() > 0
					&& changingPanel.getHeight() > 0
					&& (infoBuffer == null
							|| infoBuffer.getWidth() != changingPanel
									.getWidth() || infoBuffer.getHeight() != changingPanel
							.getHeight())) {
				if (infoBuffer != null) {
					infoBuffer.flush();
				}
				infoBuffer = createVolatileImage(changingPanel.getWidth(),
						changingPanel.getHeight());
			}
			dirty.set(true);
		}
	}

	/**
	 * Loads the given user's profile image.
	 * 
	 * @param m
	 *            the user to load
	 * @throws IOException
	 *             if the file fails to load
	 */
	private void loadUserProfile(Member m) throws IOException {
		images.put(m.getUUID(),
				ImageIO.read(new File("data/mugs/" + m.getIMG())));
	}

	/**
	 * Loads the member database, a clocktime recovery image (if any),
	 * preallocated the clocktime database, and loads the image.
	 */
	public void loadDB() {
		synchronized (bufferLock) {
			System.out.println("Loading db...");
			try {
				memDB.read();
			} catch (Exception e) {
			}
			System.out.println("Checking for recovery image...");
			try {
				String date = Util.formatDate(new Date(System
						.currentTimeMillis()));
				File clocks = new File("data/time_chunk_" + date + ".csv");
				clockDB.load(clocks, memDB, false);
			} catch (IOException e) {
			}

			System.out.println("Preallocating clock db...");
			for (Member m : memDB) {
				if (clockDB.getClocktime(m).isClockedIn()) {
					if (m.isInGroup(MemberGroup.LEAD)) {
						clockedIn.add(0, m);
					} else if (m.isInGroup(MemberGroup.SUBLEAD)) {
						// Find the last lead; after that
						int i = 0;
						for (i = 0; i < clockedIn.size(); i++) {
							if (!clockedIn.get(i).isInGroup(MemberGroup.LEAD)) {
								break;
							}
						}
						clockedIn.add(i, m);
					} else {
						clockedIn.add(m);
					}
				}
			}

			System.out.println("Checking user images...");
			try {
				CreateMugs.run(memDB);
			} catch (IOException e1) {
			}

			System.out.println("Loading user images...");
			int i = 0;
			for (Member m : memDB) {
				try {
					loadUserProfile(m);
				} catch (IOException e) {
					System.out.println("Failed to load image for "
							+ m.getName() + " -> " + m.getIMG() + "\t(" + (i++)
							+ "/" + memDB.size());
				}
			}
		}
	}

	/**
	 * Gets the currently in-use instance of the member database.
	 * 
	 * @return the member database
	 */
	public MemberDatabase getMembers() {
		return memDB;
	}

	/**
	 * Gets the currently in-use instance of the clock database.
	 * 
	 * @return the clock database
	 */
	public ClocktimeDatabase getClock() {
		return clockDB;
	}
}
