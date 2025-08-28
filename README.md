# Student Tracking System (JavaFX, SQLite) — SLP Management

![Status](https://img.shields.io/badge/status-in%20development-informational)
![Platform](https://img.shields.io/badge/platform-desktop-brightgreen)
![Tech](https://img.shields.io/badge/tech-JavaFX%20%7C%20SQLite-blue)
![Contributions](https://img.shields.io/badge/contributions-closed-lightgrey)

A modern, local **JavaFX-based** application that replaces paper records for **Short Learning Program (SLP)** student management. The system digitizes registration, module linking, progress tracking, follow-ups, and graduation workflows—reducing admin workload and improving accuracy and visibility across the team.

> **Sole-maintained project.** Contributions are **not** open at this time. This repository is for **showcase** purposes.

---

## ✨ Key Features

### Technical
- **Role-Based Access Control**: Developer, Manager, Training Advisor, Receptionist, and Staff roles with tailored permissions.
- **Local Data Store**: SQLite backend with **custom encryption** (no SQLCipher). Automated **backup & restore** utilities.
- **Automation**: Auto-assign modules by SLP, automatic graduation tagging when all modules complete, reminder scheduling.
- **Integrated Email**: Follow-up reminders, transcript requests (to SCAA), and planned student notifications.
- **Reporting**: Custom templates for **Progress**, **Follow-ups**, and **Graduation** reports.
- **UX**: JavaFX UI (Scene Builder), modern layout, filtering by name, student number, and course.

### Business Impact
- **Efficiency**: Removes paper-based processes and manual duplication.
- **Accuracy**: Single source of truth for student status, notes, and follow-ups.
- **Transparency**: Real-time visibility of progress for authorized staff.
- **Scalability**: Structured enrollment and monitoring support larger student cohorts.

---

## 🧭 Roles & Permissions (RBAC)
- **Developer** – Full access; manage modules/SLPs and data migration tasks.
- **Training Advisor** – High-level access; edit student data, send emails, run reports.
- **Manager** – Moderate access; view overviews and generate reports.
- **Receptionist** – Entry-level; register new students and print records.
- **Staff** – Basic viewing of student profiles and progress only.

---

## 🏗️ Architecture & Tech Stack
- **Language/UI**: Java 17+, JavaFX (FXML via Scene Builder)
- **Database**: SQLite with custom encryption layer
- **Email**: SMTP integration (configurable per environment)
- **Packaging**: Maven/Gradle (choose one), fat-jar or native image (planned)
- **OS**: Windows-first support (cross-platform UI considered)


## 🚀 Getting Started

### Prerequisites
- **Java 17+** installed (`java -version`)
- **Maven** or **Gradle**
- **SQLite** (bundled; no external server required)

## 📊 Core Workflows
- **Register Student** → **Auto-assign SLP modules** → **Track progress & notes** → **Follow-up reminders** → **Auto-graduate when complete** → **Email SCAA for transcript**.
- **View Students** with filters (name, student number, course); double-click to open **virtual record card** with edit, notes, and follow-up actions.

---

## 🛡️ Data Protection
- Local-only by design; no cloud sync.
- SQLite with custom encryption layer for at-rest data.
- Backups use encrypted archives; restore tool validates integrity.

---

## 🗺️ Roadmap
- Web-based edition for multi-user environments.
- Student self-service portal (view progress, request transcripts).
- Advanced analytics & dashboards (completion rates, advisor workload).
- Role-based notification center.
- PDF export for record cards and reports.


## 🤝 Contributions
This is a **single-contributor** project maintained by **Cole Lopez**. External contributions, issues, and PRs are not accepted at this time.

If you have feedback or would like a demo, please open a **discussion** (if enabled) or reach out directly.

---

## 📜 License
If the repository does **not** include a license file, then all rights are reserved by default. You may **view** the code for learning and evaluation, but **reuse, distribution, or modification** is not permitted without explicit permission.

---

## 🧩 Credits
Design and development by **Cole Lopez**.
