# Library Management System (Java Swing)

## Abstract
A desktop application that automates daily library work: cataloguing books, registering members,
issuing/returning books, calculating late fines and keeping a transaction history. It has an animated,
modern GUI built only with Java Swing/AWT (no external libraries).

## Objectives
- Replace manual registers with a fast, searchable digital system
- Track issued books, due dates and overdue fines automatically
- Give librarians a visual dashboard of library status

## Modules
1. Login (admin / admin123) with shake animation on wrong password
2. Splash screen with animated progress bar
3. Dashboard - animated stat cards and bar chart
4. Books - add, delete, search, issue, return (fine = Rs. 2/day after 14 days)
5. Members - add, delete, borrowed count
6. History - log of every issue/return, export to CSV

## Technologies
Java (JDK 11+), Swing, AWT Graphics2D, javax.swing.Timer (animation), Java Serialization (file storage)

## Run in VS Code
1. Install JDK 17+ and the "Extension Pack for Java" in VS Code.
2. Create a folder, put `LibraryApp.java` in it, open the folder in VS Code.
3. Open `LibraryApp.java` and click **Run** above `main`, or press F5.
   Terminal alternative: `javac LibraryApp.java` then `java LibraryApp`
4. If you used an older version, delete `library.dat` once.

## Future scope
MySQL/JDBC database, roles for students, barcode scanning, email reminders, PDF reports.

## Added in v3
Donut chart, overdue card, recent-activity feed, toast notifications, typing header animation and an animated About page.

## Added in v4
Procedurally drawn book covers (dashboard shelf with hover-lift, large preview beside the Books table),
smooth animated line chart (issues in last 7 days), redesigned premium dashboard cards.
Delete `library.dat` once to load demo data for the charts.
