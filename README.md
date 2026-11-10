# ATTENDLY – Smart Student Attendance & Management System

A full-stack, role-based college attendance and academic management platform built with **React.js + Tailwind CSS + Recharts** (Frontend), **Python FastAPI + SQLAlchemy** (Backend), and **PostgreSQL / MySQL** (Database, with automatic zero-config SQLite fallback for instant local demos).

## Project Folder Structure

```text
attendly/
├── README.md
├── database/
│   └── schema.sql                 # PostgreSQL & MySQL DDL schema
├── backend/
│   ├── .env.example               # Database, JWT & SMTP email settings template
│   ├── requirements.txt           # Python FastAPI & SQLAlchemy dependencies
│   ├── main.py                    # Complete FastAPI application, 6 modules & auto-seeding
│   └── uploads/                   # Uploaded medical certificates & doubt attachments
└── frontend/
    ├── package.json               # React 18, Tailwind CSS, Recharts, Lucide Icons
    ├── vite.config.js             # Vite bundler config
    ├── tailwind.config.js         # Navy blue, white & purple accent theme config
    ├── postcss.config.js          # PostCSS config
    ├── index.html                 # HTML entry point
    └── src/
        ├── main.jsx               # React DOM mount
        ├── index.css              # Tailwind CSS directives
        └── App.jsx                # Complete UI for all 3 roles & 6 modules
```

## Quick Start in VS Code

### 1. Backend Setup (FastAPI + Database)
Open a terminal in VS Code (`Ctrl + ~` or `Cmd + J`):
```bash
cd attendly/backend
python3 -m venv venv
# On macOS/Linux:
source venv/bin/activate
# On Windows:
# venv\Scripts\activate

pip install -r requirements.txt
cp .env.example .env
# Edit .env to set your PostgreSQL/MySQL DATABASE_URL (or leave default for SQLite demo mode)
uvicorn main:app --reload --port 8000
```
- Backend API runs at: `http://localhost:8000`
- Interactive Swagger API Docs: `http://localhost:8000/docs`

### 2. Frontend Setup (React + Tailwind CSS)
Open a second terminal in VS Code:
```bash
cd attendly/frontend
npm install
npm run dev
```
- Frontend runs at: `http://localhost:5173`

## Demo Credentials for College Presentation
- **Student (RED LINE <75% Demo - 68%):** `aarav@attendly.edu` / `student123`
- **Student (Green >=80% Demo - 92%):** `priya@attendly.edu` / `student123`
- **Student (Yellow Warning Demo - 76%):** `rohan.v@attendly.edu` / `student123`
- **Teacher / Faculty Role:** `ananya@attendly.edu` / `teacher123`
- **Admin Role:** `admin@attendly.edu` / `admin123`
