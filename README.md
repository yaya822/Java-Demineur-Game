# Java Minesweeper Game

A desktop Minesweeper game built with Java Swing.

The application recreates the classic Minesweeper experience with multiple difficulty levels, animated mine explosions, a timer, score calculation, and database support for saving and displaying player rankings.

## Overview

This project is a Java desktop application where the player must reveal all safe cells without clicking on a mine. The game logic is separated from the graphical interface, making the code easier to understand and maintain.

The application includes:

- A graphical interface built with Java Swing
- Minesweeper game logic for revealing cells, placing flags, detecting mines, and checking win/loss conditions
- Score calculation based on player performance and time
- MySQL database integration to save and display top scores
- Ranking tables for Easy, Medium, and Hard levels

## How to Run

### Requirements

Before running the project, make sure you have:

- Java JDK installed
- MySQL installed and running
- MySQL Connector/J available in the `lib` folder

The project uses:

```text
lib/mysql-connector-java-8.0.30.jar
```

### Database Setup

Create a MySQL database named:

```sql
Minesweeper
```

Then create a `scores` table:

```sql
CREATE TABLE scores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    score INT NOT NULL,
    time INT NOT NULL,
    difficulty VARCHAR(20) NOT NULL
);
```

Make sure the database connection information in `src/database/Connexion.java` matches your MySQL configuration:

```java
String url = "jdbc:mysql://localhost:3306/Minesweeper?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
conn = DriverManager.getConnection(url, "root", "your_password");
```

### Compile

From the project root, run:

```powershell
javac -cp ".;lib/mysql-connector-java-8.0.30.jar" -d bin src/App.java src/game/*.java src/model/*.java src/database/*.java src/ui/*.java
```

### Run

```powershell
java -cp "bin;src;lib/mysql-connector-java-8.0.30.jar" App
```

## Features

- Classic Minesweeper gameplay
- Easy, Medium, and Hard difficulty levels
- Left-click to reveal cells
- Right-click to place or remove flags
- Timer system
- Score calculation
- Win and game-over detection
- Animated explosion effect when a mine is revealed
- Custom digital font
- MySQL score saving
- Top 10 ranking table by difficulty level
- Java Swing desktop interface

## Technologies Used

- Java
- Java Swing
- MySQL
- JDBC
- MySQL Connector/J

## Project Structure

```text
src/
├── App.java
├── database/
│   ├── Connexion.java
│   └── ScoreDAO.java
├── game/
│   └── Game.java
├── model/
│   ├── Board.java
│   ├── Cellule.java
│   └── Score.java
└── ui/
    └── Minesweeper.java
```