"""
app.py — Flask entry point wiring Predictions + Chatbot together.

Run with:  python app.py
Serves on http://localhost:5000

Set GEMINI_API_KEY as an environment variable for conversational chatbot
replies; without it, the chatbot uses its rule-based fallback (still fully
functional — see chatbot_engine.py).
"""

from flask import Flask
from flask_cors import CORS

from routes import predictions_bp, chatbot_bp

app = Flask(__name__)
CORS(app)

app.register_blueprint(predictions_bp)
app.register_blueprint(chatbot_bp)


@app.route("/api/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    app.run(debug=True, port=5000)
