# Swing Image Editor

A desktop-based image editing application developed using **Java Swing**. The project focuses on applying image-processing operations through a graphical user interface while following Object-Oriented Programming principles and design patterns.

## Overview

Swing Image Editor allows users to load, view, modify, and save images through an interactive desktop application.

The application is being developed using Java Swing for the GUI and Java's built-in image-processing capabilities.

## Features

### Planned Features

- Open and display images.
- Save edited images.
- Grayscale filter.
- Image color inversion.
- Brightness adjustment.
- Image rotation.
- Undo and Redo operations.
- Layer management.
- Interactive image canvas.

## Tech Stack

- **Language:** Java
- **GUI Framework:** Java Swing
- **Image Processing:** Java AWT and BufferedImage
- **Build Tool:** Maven
- **IDE:** IntelliJ IDEA
- **Version Control:** Git and GitHub

## Project Structure

```text
SwingImageEditor/
│
├── pom.xml
├── README.md
├── .gitignore
│
└── src/
    ├── main/
    │   ├── java/com/imageeditor/
    │   │   ├── App.java
    │   │   ├── model/
    │   │   ├── controller/
    │   │   ├── operations/
    │   │   ├── command/
    │   │   └── ui/
    │   │
    │   └── resources/
    │
    └── test/
        └── java/com/imageeditor/
```

## Architecture

The project follows a modular architecture:

- **Model:** Maintains image data and layer information.
- **View (UI):** Handles the graphical interface, canvas, and layer panel.
- **Controller:** Connects the UI with image data and processing operations.
- **Operations:** Contains image-processing algorithms and filters.
- **Command:** Handles undo and redo functionality using the Command design pattern.

## Getting Started

### Prerequisites

- JDK 17 or later
- IntelliJ IDEA (recommended)
- Maven

### Installation

1. Clone the repository:

```bash
git clone <repository-url>
```

2. Navigate to the project:

```bash
cd SwingImageEditor
```

3. Open the project in IntelliJ IDEA.

4. Allow IntelliJ to load and configure Maven dependencies.

5. Run `App.java`:

```text
src/main/java/com/imageeditor/App.java
```

## Team Development

All contributors should work on separate feature branches.

```bash
git checkout -b feature/your-feature
```

Before pushing changes:

```bash
git add .
git commit -m "Describe your changes"
git push origin feature/your-feature
```

Create a Pull Request to merge your work into the main branch.

## Project Goal

To develop a functional desktop image editor while applying Object-Oriented Programming concepts, modular software architecture, and suitable design patterns.

---

**Project:** Swing Image Editor  
**Technology:** Java Swing  
**Build:** Maven
