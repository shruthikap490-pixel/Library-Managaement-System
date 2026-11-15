import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * Library Management System - Java Swing
 * Features: animated splash, animated login (floating bubbles + shake on error), fade transitions,
 * hover-animated buttons, animated dashboard (count-up + growing bar chart),
 * book/member management, search, issue/return with fine calculation, file persistence.
 * Login: admin / admin123
 */
public class LibraryApp extends JFrame {

    // ---------- Theme ----------
    static final Color DEEP = new Color(0x1e1b4b), INDIGO = new Color(0x4338ca),
            ACCENT = new Color(0x22d3ee), LIGHT = new Color(0xf1f5f9), TEXT = new Color(0x0f172a);
    static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14), BOLD = new Font("Segoe UI", Font.BOLD, 14);
    static final int LOAN_DAYS = 14, FINE_PER_DAY = 2;

    // ---------- Model ----------
    static class Book implements Serializable {
        int id; String title, author, category, issuedTo; LocalDate due;
        Book(int id, String t, String a, String c) { this.id = id; title = t; author = a; category = c; }
    }
    static class Member implements Serializable {
        int id; String name, phone;
        Member(int id, String n, String p) { this.id = id; name = n; phone = p; }
        public String toString() { return name; }
    }
    static class Data implements Serializable {
        List<Book> books = new ArrayList<>(); List<Member> members = new ArrayList<>();
        int nextBook = 1, nextMember = 1;
        List<String[]> history = new ArrayList<>();
    }

    static Data data = new Data();
    static final File FILE = new File("library.dat");

    static void load() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            data = (Data) in.readObject();
        } catch (Exception e) {
            String[][] s = {{"Wings of Fire", "A.P.J. Abdul Kalam", "Biography"}, {"Clean Code", "Robert Martin", "Technology"},
                {"Head First Java", "Kathy Sierra", "Technology"}, {"The Alchemist", "Paulo Coelho", "Fiction"},
                {"Ponniyin Selvan", "Kalki", "History"}, {"A Brief History of Time", "Stephen Hawking", "Science"}};
            for (String[] r : s) data.books.add(new Book(data.nextBook++, r[0], r[1], r[2]));
            data.members.add(new Member(data.nextMember++, "Arun Kumar", "9876543210"));
            data.members.add(new Member(data.nextMember++, "Priya S", "9123456780"));
            int[] ago = {6, 6, 5, 4, 4, 4, 3, 2, 2, 2, 1, 1, 0, 0, 0, 0};
            for (int i = 0; i < ago.length; i++)
                data.history.add(new String[]{LocalDate.now().minusDays(ago[i]).toString(), i % 3 == 2 ? "RETURNED" : "ISSUED",
                        data.books.get(i % data.books.size()).title, i % 2 == 0 ? "Arun Kumar" : "Priya S", "0"});
            save();
        }
    }
    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) { out.writeObject(data); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // ---------- Toast notification (slides down, then up) ----------
    static void toast(Component src, String text) {
        JRootPane rp = SwingUtilities.getRootPane(src);
        if (rp == null) return;
        JLayeredPane lp = rp.getLayeredPane();
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; quality(g);
                g.setPaint(new GradientPaint(0, 0, DEEP, getWidth(), 0, new Color(0x0891b2)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22); super.paintComponent(g0);
            }
        };
        l.setForeground(Color.WHITE); l.setFont(BOLD);
        int w = 330, h = 44, x = lp.getWidth() - w - 20; int[] st = {0};
        l.setBounds(x, -h, w, h); lp.add(l, JLayeredPane.POPUP_LAYER);
        new Timer(15, e -> {
            st[0]++;
            int y = st[0] <= 20 ? (int) (-h + (20 + h) * (1 - Math.pow(1 - st[0] / 20.0, 3))) : st[0] <= 120 ? 20 : (int) (20 - (20 + h) * ((st[0] - 120) / 20.0));
            l.setLocation(x, y); lp.repaint();
            if (st[0] >= 140) { ((Timer) e.getSource()).stop(); lp.remove(l); lp.repaint(); }
        }).start();
    }
    static void wrap(Graphics2D g, String text, int x, int y, int w) {
        FontMetrics fm = g.getFontMetrics(); String line = "";
        for (String word : text.split(" ")) {
            if (fm.stringWidth(line + word) > w) { g.drawString(line, x, y); y += fm.getHeight(); line = ""; }
            line += word + " ";
        }
        g.drawString(line, x, y);
    }

    // ---------- Reusable animated components ----------
    static Color lerp(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Button that smoothly blends colour and grows on hover. */
    static class FancyButton extends JButton {
        float t = 0; boolean over; final Color base; final Timer tm;
        FancyButton(String text, Color base) {
            super(text); this.base = base;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setForeground(Color.WHITE); setFont(BOLD); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tm = new Timer(15, e -> {
                t = Math.max(0, Math.min(1, t + (over ? 0.12f : -0.12f))); repaint();
                if ((over && t >= 1) || (!over && t <= 0)) ((Timer) e.getSource()).stop();
            });
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { over = true; tm.start(); }
                public void mouseExited(MouseEvent e) { over = false; tm.start(); }
            });
        }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); quality(g);
            int grow = (int) (t * 3);
            g.setColor(lerp(base, ACCENT, t * 0.55f));
            g.fillRoundRect(grow, grow, getWidth() - grow * 2, getHeight() - grow * 2, 18, 18);
            g.dispose(); super.paintComponent(g0);
        }
    }

    /** Panel that fades its whole contents in. */
    static class FadePanel extends JPanel {
        float a = 1; Timer tm;
        FadePanel(LayoutManager l) { super(l); }
        void fadeIn() {
            a = 0; if (tm != null) tm.stop();
            tm = new Timer(15, e -> { a += 0.07f; if (a >= 1) { a = 1; tm.stop(); } repaint(); });
            tm.start();
        }
        public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, a)));
            super.paint(g); g.dispose();
        }
    }

    static class GradientPanel extends JPanel {
        final Color c1, c2; final boolean vertical;
        GradientPanel(Color c1, Color c2, boolean vertical) { this.c1 = c1; this.c2 = c2; this.vertical = vertical; }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setPaint(new GradientPaint(0, 0, c1, vertical ? 0 : getWidth(), vertical ? getHeight() : getHeight(), c2));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ---------- Login screen ----------
    static class LoginPanel extends GradientPanel {
        final float[] x = new float[18], y = new float[18], r = new float[18], v = new float[18];
        int shake = 0;
        final JTextField user = new JTextField("admin"); final JPasswordField pass = new JPasswordField();
        final JLabel msg = new JLabel(" ", SwingConstants.CENTER);

        LoginPanel(Runnable onSuccess) {
            super(DEEP, INDIGO, true);
            setLayout(new GridBagLayout());
            Random rnd = new Random();
            for (int i = 0; i < x.length; i++) {
                x[i] = rnd.nextInt(1200); y[i] = rnd.nextInt(800); r[i] = 15 + rnd.nextInt(60); v[i] = 0.4f + rnd.nextFloat() * 1.2f;
            }
            new Timer(30, e -> {
                for (int i = 0; i < x.length; i++) { y[i] -= v[i]; if (y[i] < -r[i]) { y[i] = getHeight() + r[i]; x[i] = rnd.nextInt(Math.max(1, getWidth())); } }
                repaint();
            }).start();

            JPanel card = new JPanel(new GridLayout(0, 1, 0, 10)) {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0; quality(g);
                    g.setColor(new Color(255, 255, 255, 235)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                }
                public void paint(Graphics g) { g.translate(shake, 0); super.paint(g); }
            };
            card.setOpaque(false); card.setBorder(new EmptyBorder(30, 40, 30, 40));
            JLabel title = new JLabel("\uD83D\uDCDA Library Manager", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 26)); title.setForeground(DEEP);
            JLabel sub = new JLabel("Sign in to continue", SwingConstants.CENTER); sub.setFont(BODY);
            user.setFont(BODY); pass.setFont(BODY);
            user.setBorder(BorderFactory.createTitledBorder("Username"));
            pass.setBorder(BorderFactory.createTitledBorder("Password"));
            msg.setForeground(Color.RED);
            FancyButton login = new FancyButton("Login", INDIGO);
            ActionListener go = e -> {
                if (user.getText().equals("admin") && new String(pass.getPassword()).equals("admin123")) onSuccess.run();
                else { msg.setText("Invalid credentials (hint: admin / admin123)"); shake(card); }
            };
            login.addActionListener(go); pass.addActionListener(go);
            card.add(title); card.add(sub); card.add(user); card.add(pass); card.add(msg); card.add(login);
            add(card);
        }
        void shake(JComponent card) {
            int[] step = {0};
            new Timer(25, e -> {
                shake = (int) (Math.sin(step[0] * 1.5) * 12 * (1 - step[0] / 16f));
                card.repaint();
                if (++step[0] > 16) { shake = 0; card.repaint(); ((Timer) e.getSource()).stop(); }
            }).start();
        }
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; quality(g);
            g.setColor(new Color(255, 255, 255, 30));
            for (int i = 0; i < x.length; i++) g.fillOval((int) (x[i] - r[i]), (int) (y[i] - r[i]), (int) (r[i] * 2), (int) (r[i] * 2));
        }
    }

    // ---------- Book cover artwork (drawn with Java2D, no image files needed) ----------
    static final int[][] COVER_COLORS = {{0x7c3aed, 0xdb2777}, {0x0891b2, 0x1e3a8a}, {0xea580c, 0xdc2626}, {0x059669, 0x0f766e},
            {0x4338ca, 0x0ea5e9}, {0xb45309, 0x78350f}, {0xbe123c, 0x4c1d95}, {0x0f172a, 0x475569}};

    static void drawCover(Graphics2D g, Book b, int x, int y, int w, int h) {
        int[] c = COVER_COLORS[Math.abs(b.category.hashCode()) % COVER_COLORS.length];
        Shape old = g.getClip();
        g.setColor(new Color(0, 0, 0, 45)); g.fillRoundRect(x + 4, y + 6, w, h, 12, 12);
        g.setClip(new java.awt.geom.RoundRectangle2D.Double(x, y, w, h, 12, 12));
        g.setPaint(new GradientPaint(x, y, new Color(c[0]), x + w, y + h, new Color(c[1]))); g.fillRect(x, y, w, h);
        g.setColor(new Color(255, 255, 255, 38)); g.fillOval(x + w / 2, y - w / 4, w, w); g.fillOval(x - w / 3, y + h - w / 2, w, w);
        int sp = w / 12 + 2;
        g.setColor(new Color(0, 0, 0, 70)); g.fillRect(x, y, sp, h);
        g.setColor(new Color(255, 255, 255, 90)); g.fillRect(x + sp, y, 2, h);
        g.setFont(new Font("Georgia", Font.BOLD, Math.max(10, w / 9)));
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>(); String line = "";
        for (String word : b.title.split(" ")) {
            if (!line.isEmpty() && fm.stringWidth(line + word) > w - sp - 16) { lines.add(line.trim()); line = ""; }
            line += word + " ";
        }
        lines.add(line.trim());
        int ty = y + h / 3 - lines.size() * fm.getHeight() / 2 + fm.getAscent();
        g.setColor(Color.WHITE);
        for (String l : lines) { g.drawString(l, x + sp + (w - sp - fm.stringWidth(l)) / 2, ty); ty += fm.getHeight(); }
        g.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(9, w / 12))); fm = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 225));
        g.drawString(b.author, x + sp + Math.max(4, (w - sp - fm.stringWidth(b.author)) / 2), y + h - 12);
        g.setClip(old);
    }
    static Book find(int id) { return data.books.stream().filter(b -> b.id == id).findFirst().orElse(null); }

    /** Large cover preview shown beside the books table (fades + slides in on selection). */
    static class CoverView extends JPanel {
        Book b; float a = 1; Timer tm;
        CoverView() { setPreferredSize(new Dimension(240, 0)); setOpaque(false); }
        void showBook(Book nb) {
            b = nb; a = 0; if (tm != null) tm.stop();
            tm = new Timer(15, e -> { a += 0.08f; if (a >= 1) { a = 1; tm.stop(); } repaint(); }); tm.start();
        }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0; quality(g);
            g.setColor(Color.WHITE); g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            if (b == null) { g.setColor(Color.GRAY); g.setFont(BODY); g.drawString("Select a book to preview", 28, 40); return; }
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, a)));
            int w = 160, h = 228, x = (getWidth() - w) / 2, y = 22 + (int) ((1 - a) * 28);
            drawCover(g, b, x, y, w, h);
            int ty = y + h + 34;
            g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 16)); wrap(g, b.title, 20, ty, getWidth() - 40);
            g.setFont(BODY); g.setColor(Color.GRAY);
            g.drawString("by " + b.author, 20, ty + 40); g.drawString("Category: " + b.category, 20, ty + 62);
            g.setColor(b.issuedTo == null ? new Color(0x059669) : new Color(0xdc2626)); g.setFont(BOLD);
            g.drawString(b.issuedTo == null ? "Available" : "Issued to " + b.issuedTo, 20, ty + 86);
            if (b.due != null) { g.setFont(BODY); g.drawString("Due: " + b.due, 20, ty + 106); }
        }
    }

    // ---------- Dashboard ----------
    static class Dashboard extends JPanel {
        float p = 0; Timer tm; int hover = -1;
        final List<Rectangle> rects = new ArrayList<>();
        Dashboard() {
            setBackground(LIGHT);
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    int hv = -1;
                    for (int i = 0; i < rects.size(); i++) if (rects.get(i).contains(e.getPoint())) hv = i;
                    if (hv != hover) { hover = hv; setCursor(Cursor.getPredefinedCursor(hv >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); repaint(); }
                }
            });
        }
        void play() {
            p = 0; if (tm != null) tm.stop();
            tm = new Timer(16, e -> { p += 0.02f; if (p >= 1) { p = 1; tm.stop(); } repaint(); });
            tm.start();
        }
        void panel(Graphics2D g, int x, int y, int w, int h, String title) {
            g.setColor(new Color(0, 0, 0, 18)); g.fillRoundRect(x + 2, y + 4, w, h, 22, 22);
            g.setColor(Color.WHITE); g.fillRoundRect(x, y, w, h, 22, 22);
            g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 16)); g.drawString(title, x + 20, y + 30);
        }
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; quality(g);
            float ease = 1 - (float) Math.pow(1 - p, 3);
            LocalDate today = LocalDate.now();
            long issued = data.books.stream().filter(b -> b.issuedTo != null).count();
            long overdue = data.books.stream().filter(b -> b.due != null && b.due.isBefore(today)).count();
            int[] vals = {data.books.size(), (int) issued, data.books.size() - (int) issued, data.members.size(), (int) overdue};
            String[] labels = {"Total Books", "Issued Books", "Available", "Members", "Overdue"}, icons = {"B", "I", "A", "M", "!"};
            Color[] cols = {new Color(0x6366f1), new Color(0xf97316), new Color(0x10b981), new Color(0x06b6d4), new Color(0xe11d48)};
            int gap = 20, W = getWidth(), cw = (W - gap * 6) / 5;
            // ---- row 1: stat cards
            for (int i = 0; i < 5; i++) {
                int x = gap + i * (cw + gap), y = 15 + (int) ((1 - ease) * 30);
                panel(g, x, y, cw, 95, "");
                g.setColor(cols[i]); g.fillRoundRect(x + 12, y + 88, cw - 24, 5, 5, 5);
                g.setColor(lerp(cols[i], Color.WHITE, 0.82f)); g.fillRoundRect(x + 14, y + 16, 40, 40, 12, 12);
                g.setColor(cols[i]); g.setFont(new Font("Segoe UI", Font.BOLD, 20)); g.drawString(icons[i], x + 28, y + 44);
                g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 30)); g.drawString("" + (int) (vals[i] * ease), x + 66, y + 46);
                g.setColor(Color.GRAY); g.setFont(BODY); g.drawString(labels[i], x + 14, y + 78);
            }
            // ---- row 2: three charts
            int top = 130, ph = 235, pw = (W - gap * 4) / 3, x1 = gap, x2 = gap * 2 + pw, x3 = gap * 3 + pw * 2;
            panel(g, x1, top, pw, ph, "Books by Category");
            Map<String, Integer> cat = new TreeMap<>();
            for (Book b : data.books) cat.merge(b.category, 1, Integer::sum);
            int bBase = top + ph - 30, bTop = top + 60, mx = cat.isEmpty() ? 1 : Collections.max(cat.values());
            int slot = (pw - 40) / Math.max(1, cat.size()), bw = Math.min(38, slot - 12), bx = x1 + 20 + (slot - bw) / 2;
            for (Map.Entry<String, Integer> en : cat.entrySet()) {
                int bh = (int) ((bBase - bTop) * en.getValue() / (float) mx * ease);
                g.setPaint(new GradientPaint(bx, bBase - bh, ACCENT, bx, bBase, INDIGO)); g.fillRoundRect(bx, bBase - bh, bw, bh, 10, 10);
                g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 12)); g.drawString("" + en.getValue(), bx + bw / 2 - 4, bBase - bh - 5);
                g.setColor(Color.GRAY); g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                String lb = en.getKey().length() > 8 ? en.getKey().substring(0, 7) + "." : en.getKey();
                g.drawString(lb, bx + bw / 2 - g.getFontMetrics().stringWidth(lb) / 2, bBase + 16); bx += slot;
            }
            g.setColor(new Color(0xe2e8f0)); g.drawLine(x1 + 15, bBase, x1 + pw - 15, bBase);

            panel(g, x2, top, pw, ph, "Books Overview");
            int cx = x2 + pw / 2, cy = top + 120, R = 52; double total = Math.max(1, vals[0]);
            double iAng = 360.0 * issued / total * ease, aAng = 360.0 * (total - issued) / total * ease;
            g.setStroke(new BasicStroke(24, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.setColor(new Color(0xf97316)); g.draw(new java.awt.geom.Arc2D.Double(cx - R, cy - R, R * 2, R * 2, 90, -iAng, java.awt.geom.Arc2D.OPEN));
            g.setColor(new Color(0x10b981)); g.draw(new java.awt.geom.Arc2D.Double(cx - R, cy - R, R * 2, R * 2, 90 - iAng, -aAng, java.awt.geom.Arc2D.OPEN));
            g.setStroke(new BasicStroke(1));
            g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            String pct = (int) (100 * issued / total * ease) + "%";
            g.drawString(pct, cx - g.getFontMetrics().stringWidth(pct) / 2, cy + 8);
            g.setFont(BODY);
            g.setColor(new Color(0xf97316)); g.fillOval(x2 + 24, top + ph - 31, 10, 10); g.setColor(TEXT); g.drawString("Issued " + issued, x2 + 40, top + ph - 21);
            g.setColor(new Color(0x10b981)); g.fillOval(x2 + pw / 2 + 4, top + ph - 31, 10, 10); g.setColor(TEXT); g.drawString("Available " + (vals[0] - issued), x2 + pw / 2 + 20, top + ph - 21);

            panel(g, x3, top, pw, ph, "Issues - Last 7 Days");
            int[] cnt = new int[7];
            for (String[] hh : data.history) if (hh[1].equals("ISSUED")) {
                long d = ChronoUnit.DAYS.between(LocalDate.parse(hh[0]), today);
                if (d >= 0 && d < 7) cnt[6 - (int) d]++;
            }
            int cmax = 1; for (int c : cnt) cmax = Math.max(cmax, c);
            int lx = x3 + 30, lw = pw - 60, lb = top + ph - 38, lt = top + 65;
            double[] px = new double[7], py = new double[7];
            for (int i = 0; i < 7; i++) { px[i] = lx + lw * i / 6.0; py[i] = lb - (lb - lt) * 0.92 * cnt[i] / cmax; }
            g.setColor(new Color(0xe2e8f0));
            for (int i = 0; i < 4; i++) g.drawLine(lx - 10, lb - (lb - lt) * i / 3, lx + lw + 10, lb - (lb - lt) * i / 3);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11)); g.setColor(Color.GRAY);
            for (int i = 0; i < 7; i++) {
                String dn = today.minusDays(6 - i).getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
                g.drawString(dn, (int) px[i] - 9, lb + 18);
            }
            Shape oc = g.getClip(); g.setClip(x3, top, (int) (pw * ease), ph);
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double(); path.moveTo(px[0], py[0]);
            for (int i = 1; i < 7; i++) { double mxp = (px[i - 1] + px[i]) / 2; path.curveTo(mxp, py[i - 1], mxp, py[i], px[i], py[i]); }
            java.awt.geom.Path2D.Double fill = new java.awt.geom.Path2D.Double(path);
            fill.lineTo(px[6], lb); fill.lineTo(px[0], lb); fill.closePath();
            g.setPaint(new GradientPaint(0, lt, new Color(0x4338ca, false).brighter(), 0, lb, new Color(255, 255, 255, 0)));
            g.setComposite(AlphaComposite.SrcOver.derive(0.28f)); g.fill(fill); g.setComposite(AlphaComposite.SrcOver);
            g.setColor(INDIGO); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path); g.setStroke(new BasicStroke(1));
            for (int i = 0; i < 7; i++) {
                g.setColor(Color.WHITE); g.fillOval((int) px[i] - 5, (int) py[i] - 5, 10, 10);
                g.setColor(INDIGO); g.drawOval((int) px[i] - 5, (int) py[i] - 5, 10, 10);
                if (cnt[i] > 0) { g.setColor(TEXT); g.drawString("" + cnt[i], (int) px[i] - 3, (int) py[i] - 10); }
            }
            g.setClip(oc);
            // ---- row 3: animated book shelf with covers
            int sy = top + ph + 20, sh = getHeight() - sy - 15;
            panel(g, gap, sy, W - gap * 2, sh, "Book Shelf  (hover a cover)");
            rects.clear();
            int ch = Math.max(60, Math.min(140, sh - 90)), cwd = ch * 5 / 7;
            int n = Math.max(0, Math.min(data.books.size(), (W - gap * 2 - 40) / (cwd + 18)));
            for (int i = 0; i < n; i++) {
                Book b = data.books.get(i);
                float t = Math.max(0, Math.min(1, p * (n + 3) - i));
                int x = gap + 22 + i * (cwd + 18), y0 = sy + 44, y = y0 + (int) ((1 - t) * 30) - (hover == i ? 8 : 0);
                rects.add(new Rectangle(x, y0 - 8, cwd, ch + 8));
                g.setComposite(AlphaComposite.SrcOver.derive(t));
                drawCover(g, b, x, y, cwd, ch);
                g.setFont(new Font("Segoe UI", Font.BOLD, 12)); g.setColor(b.issuedTo == null ? new Color(0x059669) : new Color(0xdc2626));
                g.drawString(b.issuedTo == null ? "Available" : "Issued", x, y + ch + 18);
                g.setComposite(AlphaComposite.SrcOver);
            }
        }
    }

    // ---------- Table helper ----------
    static JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m); t.setRowHeight(30); t.setFont(BODY); t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0)); t.setSelectionBackground(new Color(0xc7d2fe));
        t.setSelectionForeground(TEXT);
        JTableHeader h = t.getTableHeader(); h.setBackground(INDIGO); h.setForeground(Color.WHITE); h.setFont(BOLD);
        h.setPreferredSize(new Dimension(0, 34));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, f, r, c);
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xeef2ff));
                setForeground("Issued".equals(v) ? new Color(0xdc2626) : "Available".equals(v) ? new Color(0x059669) : TEXT);
                if (v instanceof LocalDate && ((LocalDate) v).isBefore(LocalDate.now())) { setForeground(new Color(0xdc2626)); setFont(BOLD); }
                else setFont(BODY);
                setBorder(new EmptyBorder(0, 10, 0, 10)); return this;
            }
        });
        return t;
    }

    // ---------- Books panel ----------
    static class BooksPanel extends JPanel {
        final DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Category", "Status", "Member", "Due"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        final JTable t = styledTable(m); final JTextField search = new JTextField(18); final Runnable onChange; final CoverView cover = new CoverView();

        BooksPanel(Runnable onChange) {
            this.onChange = onChange; setLayout(new BorderLayout(10, 10)); setBackground(LIGHT); setBorder(new EmptyBorder(15, 15, 15, 15));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); top.setOpaque(false);
            search.setFont(BODY); search.setBorder(BorderFactory.createTitledBorder("\uD83D\uDD0D Search title / author / category"));
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            });
            top.add(search);
            FancyButton add = new FancyButton("+ Add", INDIGO), del = new FancyButton("Delete", new Color(0xdc2626)),
                    issue = new FancyButton("Issue", new Color(0xea580c)), ret = new FancyButton("Return", new Color(0x059669));
            for (FancyButton b : new FancyButton[]{add, del, issue, ret}) { b.setPreferredSize(new Dimension(90, 40)); top.add(b); }
            add.addActionListener(e -> addBook()); del.addActionListener(e -> deleteBook());
            issue.addActionListener(e -> issueBook()); ret.addActionListener(e -> returnBook());
            t.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                int r = t.getSelectedRow(); cover.showBook(r < 0 ? null : find((int) m.getValueAt(r, 0)));
            });
            add(top, BorderLayout.NORTH); add(new JScrollPane(t), BorderLayout.CENTER); add(cover, BorderLayout.EAST); refresh();
        }
        void refresh() {
            m.setRowCount(0); String q = search.getText().toLowerCase();
            for (Book b : data.books)
                if ((b.title + b.author + b.category).toLowerCase().contains(q))
                    m.addRow(new Object[]{b.id, b.title, b.author, b.category, b.issuedTo == null ? "Available" : "Issued",
                            b.issuedTo == null ? "-" : b.issuedTo, b.due == null ? "-" : b.due});
        }
        Book selected() {
            int r = t.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(this, "Select a book first."); return null; }
            int id = (int) m.getValueAt(r, 0);
            return data.books.stream().filter(b -> b.id == id).findFirst().orElse(null);
        }
        void done() { save(); refresh(); onChange.run(); toast(this, "Saved successfully"); }
        void addBook() {
            JTextField ti = new JTextField(), au = new JTextField(), ca = new JTextField();
            Object[] f = {"Title", ti, "Author", au, "Category", ca};
            if (JOptionPane.showConfirmDialog(this, f, "Add Book", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
                    && !ti.getText().isBlank()) {
                data.books.add(new Book(data.nextBook++, ti.getText().trim(), au.getText().trim(), ca.getText().isBlank() ? "General" : ca.getText().trim()));
                done();
            }
        }
        void deleteBook() {
            Book b = selected();
            if (b == null) return;
            if (b.issuedTo != null) { JOptionPane.showMessageDialog(this, "Book is currently issued."); return; }
            data.books.remove(b); done();
        }
        void issueBook() {
            Book b = selected();
            if (b == null) return;
            if (b.issuedTo != null) { JOptionPane.showMessageDialog(this, "Already issued to " + b.issuedTo); return; }
            if (data.members.isEmpty()) { JOptionPane.showMessageDialog(this, "Add a member first."); return; }
            JComboBox<Member> box = new JComboBox<>(data.members.toArray(new Member[0]));
            if (JOptionPane.showConfirmDialog(this, box, "Issue to member", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                b.issuedTo = ((Member) box.getSelectedItem()).name; b.due = LocalDate.now().plusDays(LOAN_DAYS);
                data.history.add(new String[]{LocalDate.now().toString(), "ISSUED", b.title, b.issuedTo, "0"}); done();
            }
        }
        void returnBook() {
            Book b = selected();
            if (b == null) return;
            if (b.issuedTo == null) { JOptionPane.showMessageDialog(this, "This book is not issued."); return; }
            long late = Math.max(0, ChronoUnit.DAYS.between(b.due, LocalDate.now()));
            JOptionPane.showMessageDialog(this, "Returned by " + b.issuedTo + "\nLate days: " + late + "\nFine: Rs. " + late * FINE_PER_DAY);
            data.history.add(new String[]{LocalDate.now().toString(), "RETURNED", b.title, b.issuedTo, "" + late * FINE_PER_DAY});
            b.issuedTo = null; b.due = null; done();
        }
    }

    // ---------- Members panel ----------
    static class MembersPanel extends JPanel {
        final DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Books Borrowed"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        final JTable t = styledTable(m); final Runnable onChange;
        MembersPanel(Runnable onChange) {
            this.onChange = onChange; setLayout(new BorderLayout(10, 10)); setBackground(LIGHT); setBorder(new EmptyBorder(15, 15, 15, 15));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
            FancyButton add = new FancyButton("+ Add Member", INDIGO), del = new FancyButton("Delete", new Color(0xdc2626));
            add.setPreferredSize(new Dimension(140, 40)); del.setPreferredSize(new Dimension(90, 40));
            add.addActionListener(e -> {
                JTextField n = new JTextField(), p = new JTextField();
                if (JOptionPane.showConfirmDialog(this, new Object[]{"Name", n, "Phone", p}, "Add Member", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
                        && !n.getText().isBlank()) { data.members.add(new Member(data.nextMember++, n.getText().trim(), p.getText().trim())); done(); }
            });
            del.addActionListener(e -> {
                int r = t.getSelectedRow(); if (r < 0) return;
                int id = (int) m.getValueAt(r, 0);
                Member mem = data.members.stream().filter(x -> x.id == id).findFirst().get();
                if (data.books.stream().anyMatch(b -> mem.name.equals(b.issuedTo))) { JOptionPane.showMessageDialog(this, "Member has unreturned books."); return; }
                data.members.remove(mem); done();
            });
            top.add(add); top.add(del); add(top, BorderLayout.NORTH); add(new JScrollPane(t), BorderLayout.CENTER); refresh();
        }
        void done() { save(); refresh(); onChange.run(); toast(this, "Saved successfully"); }
        void refresh() {
            m.setRowCount(0);
            for (Member x : data.members)
                m.addRow(new Object[]{x.id, x.name, x.phone, data.books.stream().filter(b -> x.name.equals(b.issuedTo)).count()});
        }
    }

    // ---------- History panel ----------
    static class HistoryPanel extends JPanel {
        final DefaultTableModel m = new DefaultTableModel(new String[]{"Date", "Action", "Book", "Member", "Fine (Rs)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        HistoryPanel() {
            setLayout(new BorderLayout(10, 10)); setBackground(LIGHT); setBorder(new EmptyBorder(15, 15, 15, 15));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
            FancyButton export = new FancyButton("Export CSV", new Color(0x059669)); export.setPreferredSize(new Dimension(130, 40));
            export.addActionListener(e -> {
                try (PrintWriter w = new PrintWriter("history.csv")) {
                    w.println("Date,Action,Book,Member,Fine");
                    for (String[] h : data.history) w.println(String.join(",", h));
                    JOptionPane.showMessageDialog(this, "Saved to " + new File("history.csv").getAbsolutePath());
                } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage()); }
            });
            top.add(export); add(top, BorderLayout.NORTH); add(new JScrollPane(styledTable(m)), BorderLayout.CENTER); refresh();
        }
        void refresh() { m.setRowCount(0); for (int i = data.history.size() - 1; i >= 0; i--) m.addRow(data.history.get(i)); }
    }

    // ---------- About / Project information panel (staggered card animation) ----------
    static class InfoPanel extends JPanel {
        float p = 0; Timer tm;
        final String[][] cards = {
            {"Objective", "Automate cataloguing, issuing, returning and fine calculation so the librarian saves time and avoids paper registers."},
            {"Modules", "Login, Splash, Dashboard, Books, Members, History and About. Each module works on shared in-memory data."},
            {"Technologies", "Java (JDK 11+), Swing for widgets, AWT Graphics2D for custom painting, javax.swing.Timer for animation."},
            {"Fine Rules", "Loan period is 14 days. After that a fine of Rs. 2 per late day is calculated automatically on return."},
            {"Animations", "Fade transitions, hover colour blending, shaking login card, floating bubbles, count-up numbers, growing bars, donut sweep, toasts."},
            {"Data Storage", "Books, members and history are saved with Java Serialization in library.dat. History exports to CSV."},
            {"Security", "Admin login screen with validation and error feedback. Can be extended with hashed passwords and roles."},
            {"Future Scope", "MySQL via JDBC, barcode scanner, email or SMS reminders, student login, PDF reports, web version."},
            {"Developed By", "Your Name | Department | College Name | Academic Year (edit this text in InfoPanel)."}};
        InfoPanel() { setBackground(LIGHT); }
        void play() {
            p = 0; if (tm != null) tm.stop();
            tm = new Timer(16, e -> { p += 0.012f; if (p >= 1) { p = 1; tm.stop(); } repaint(); });
            tm.start();
        }
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; quality(g);
            int gap = 18, cw = (getWidth() - gap * 4) / 3, ch = (getHeight() - gap * 4) / 3;
            Color[] tint = {new Color(0x6366f1), new Color(0x0891b2), new Color(0x7c3aed)};
            for (int i = 0; i < cards.length; i++) {
                float t = Math.max(0, Math.min(1, p * (cards.length + 3) - i));
                int x = gap + (i % 3) * (cw + gap), y = gap + (i / 3) * (ch + gap) + (int) ((1 - t) * 40);
                g.setComposite(AlphaComposite.SrcOver.derive(t));
                g.setColor(new Color(0, 0, 0, 25)); g.fillRoundRect(x + 3, y + 4, cw, ch, 22, 22);
                g.setColor(Color.WHITE); g.fillRoundRect(x, y, cw, ch, 22, 22);
                g.setPaint(new GradientPaint(x, y, tint[i % 3], x + cw, y, lerp(tint[i % 3], ACCENT, 0.6f)));
                g.fillRoundRect(x, y, cw, 38, 22, 22); g.fillRect(x, y + 20, cw, 18);
                g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI", Font.BOLD, 16)); g.drawString(cards[i][0], x + 16, y + 26);
                g.setColor(TEXT); g.setFont(new Font("Segoe UI", Font.PLAIN, 13)); wrap(g, cards[i][1], x + 16, y + 62, cw - 32);
            }
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    // ---------- Animated splash screen ----------
    static class Splash extends JWindow {
        float p = 0;
        Splash(Runnable done) {
            setSize(520, 300); setLocationRelativeTo(null);
            JPanel pn = new JPanel() {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0; quality(g);
                    g.setPaint(new GradientPaint(0, 0, DEEP, getWidth(), getHeight(), INDIGO)); g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI", Font.BOLD, 36)); g.drawString("Library Manager", 100, 120);
                    g.setFont(BODY); g.drawString("Loading modules... " + (int) (p * 100) + "%", 100, 160);
                    g.setColor(new Color(255, 255, 255, 60)); g.fillRoundRect(60, 220, 400, 12, 12, 12);
                    g.setColor(ACCENT); g.fillRoundRect(60, 220, (int) (400 * p), 12, 12, 12);
                }
            };
            setContentPane(pn);
            new Timer(25, e -> { p += 0.012f; pn.repaint(); if (p >= 1) { ((Timer) e.getSource()).stop(); dispose(); done.run(); } }).start();
        }
    }

    // ---------- Main frame ----------
    final CardLayout rootLayout = new CardLayout(), pages = new CardLayout();
    final FadePanel root = new FadePanel(rootLayout), content = new FadePanel(pages);
    final Dashboard dashboard = new Dashboard();
    BooksPanel books; MembersPanel members; HistoryPanel history; final InfoPanel info = new InfoPanel();

    LibraryApp() {
        super("Library Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1150, 720); setLocationRelativeTo(null);
        books = new BooksPanel(this::refreshAll); members = new MembersPanel(this::refreshAll); history = new HistoryPanel();
        content.add(dashboard, "Dashboard"); content.add(books, "Books"); content.add(members, "Members"); content.add(history, "History"); content.add(info, "About");

        JPanel main = new JPanel(new BorderLayout());
        GradientPanel side = new GradientPanel(DEEP, INDIGO, true);
        side.setLayout(new GridLayout(0, 1, 0, 8)); side.setBorder(new EmptyBorder(20, 12, 20, 12)); side.setPreferredSize(new Dimension(200, 0));
        JLabel logo = new JLabel("\uD83D\uDCDA LibraryPro", SwingConstants.CENTER);
        logo.setForeground(Color.WHITE); logo.setFont(new Font("Segoe UI", Font.BOLD, 20)); side.add(logo);
        for (String n : new String[]{"Dashboard", "Books", "Members", "History", "About"}) {
            FancyButton b = new FancyButton(n, new Color(0x5b54e8)); b.addActionListener(e -> show(n)); side.add(b);
        }
        FancyButton logout = new FancyButton("Logout", new Color(0xdc2626)); logout.addActionListener(e -> rootLayout.show(root, "login"));
        side.add(new JLabel()); side.add(new JLabel()); side.add(logout);

        JLabel clock = new JLabel(); clock.setFont(BOLD); clock.setForeground(Color.WHITE); clock.setBorder(new EmptyBorder(0, 0, 0, 15));
        JLabel head = new JLabel("  Welcome, Admin"); head.setFont(new Font("Segoe UI", Font.BOLD, 18)); head.setForeground(Color.WHITE);
        String full = "  Welcome to Library Management System  |  Admin"; head.setText("");
        int[] kk = {0}; new Timer(55, e -> { head.setText(full.substring(0, ++kk[0])); if (kk[0] >= full.length()) ((Timer) e.getSource()).stop(); }).start();
        GradientPanel header = new GradientPanel(INDIGO, new Color(0x0891b2), false);
        header.setLayout(new BorderLayout()); header.setPreferredSize(new Dimension(0, 50)); header.add(head, BorderLayout.WEST); header.add(clock, BorderLayout.EAST);
        new Timer(1000, e -> clock.setText(new java.util.Date().toString().substring(0, 19))).start();

        main.add(side, BorderLayout.WEST); main.add(header, BorderLayout.NORTH); main.add(content, BorderLayout.CENTER);
        root.add(new LoginPanel(() -> { rootLayout.show(root, "main"); show("Dashboard"); root.fadeIn(); toast(root, "Welcome back, Admin!"); }), "login");
        root.add(main, "main");
        setContentPane(root);
    }

    void show(String name) { pages.show(content, name); refreshAll(); content.fadeIn(); if (name.equals("Dashboard")) dashboard.play(); if (name.equals("About")) info.play(); }
    void refreshAll() { books.refresh(); members.refresh(); history.refresh(); dashboard.repaint(); }

    public static void main(String[] args) {
        load();
        SwingUtilities.invokeLater(() -> new Splash(() -> new LibraryApp().setVisible(true)).setVisible(true));
    }
}
