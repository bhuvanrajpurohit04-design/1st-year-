# 1st Year Projects

A collection of projects built during 1st year: two standalone accessible-game demos, and a full-stack expense tracker (SIH 2026 project) with AI-powered predictions and a chatbot.

## Expense Tracker — SIH 2026 (`backend/` + `frontend/`)

A student expense tracker with:
- **Smart predictions** — weighted moving average + trend + day-of-week seasonality forecasting, a 0–100 confidence score, and category-wise spend forecasts (`backend/ai_engine.py`).
- **AI finance chatbot** — answers questions about budget, daily limit, and spending trends, grounded in the user's own numbers. Uses Gemini for natural replies when `GEMINI_API_KEY` is set, and always has a rule-based fallback so it never breaks on stage (`backend/chatbot_engine.py`).

### Architecture

```
backend/                  Flask API
  app.py                  entry point — registers blueprints, runs on :5000
  routes.py                /api/predictions/<user_id>, /api/chatbot
  ai_engine.py             PredictionEngine (forecasting)
  chatbot_engine.py        FinanceChatbot (Gemini + rule-based fallback)
  data_store.py            in-memory demo data — swap for real DB queries
  requirements.txt

frontend/                 React app
  src/App.js               renders Predictions + floating Chatbot
  src/components/
    Predictions.js/css      month-end forecast card + category chart (chart.js)
    Chatbot.js/css          floating chat widget
  package.json              proxies API calls to http://localhost:5000 in dev
```

`data_store.py` is the one thing to replace when the real database is ready — swap `get_user_expenses()` / `get_user_income()` for real SQLAlchemy queries and everything above it (predictions, chatbot) keeps working unchanged, exactly as described in the original integration notes.

### Running it

**Backend**
```bash
cd backend
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python app.py        # http://localhost:5000
```
Optional: `export GEMINI_API_KEY=...` for real conversational chatbot replies (works fine without it).

**Frontend**
```bash
cd frontend
npm install
npm start             # http://localhost:3000, proxies /api to :5000
```

Both need to be running at the same time for the app to work end to end.

### Demo tip
The demo user (id 1) is seeded in `data_store.py` with expenses across the last several days, so the prediction confidence score and trend line look right immediately — thin data (1-2 days) makes both look weak.

## Other projects

### Unity Quest (`unity-quest/`)
**One game, every ability.**

A multiplayer game hub built so players with different abilities can compete fairly in the same match. Includes player/profile setup (roster management) and adaptive play modes.

- Open `unity-quest/index.html` in a browser to run it
- Structure: `index.html`, `css/`, `js/`

### Triad Shift (`triad-shift.html`)
A security triage game with three solo play modes — **Deaf**, **Blind**, and **Mute** — each designed to let a player complete the full shift independently, regardless of ability. Built by Team Infinity Loop.

- Single-file app — just open `triad-shift.html` in a browser to play
