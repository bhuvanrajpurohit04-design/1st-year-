"""
routes.py — Flask endpoints for Predictions + Chatbot.

get_user_expenses() / get_user_income() come from data_store.py (an
in-memory demo store for now) — swap that module for real SQLAlchemy
queries when the real database is wired in, and these routes keep working.
"""

from flask import Blueprint, request, jsonify
from ai_engine import PredictionEngine
from chatbot_engine import FinanceChatbot
from data_store import get_user_expenses, get_user_income

predictions_bp = Blueprint("predictions", __name__)
chatbot_bp = Blueprint("chatbot", __name__)
chatbot = FinanceChatbot()


@predictions_bp.route("/api/predictions/<int:user_id>", methods=["GET"])
def get_predictions(user_id):
    expenses = get_user_expenses(user_id)
    income = get_user_income(user_id)

    engine = PredictionEngine(expenses)
    forecast = engine.forecast_month_end(income)
    categories = engine.category_forecast()

    return jsonify({**forecast, "category_forecast": categories})


@chatbot_bp.route("/api/chatbot", methods=["POST"])
def chatbot_reply():
    body = request.get_json(force=True)
    user_id = body.get("user_id")
    message = body.get("message", "").strip()

    if not message:
        return jsonify({"error": "message is required"}), 400

    expenses = get_user_expenses(user_id)
    income = get_user_income(user_id)
    engine = PredictionEngine(expenses)
    forecast = engine.forecast_month_end(income)
    forecast["category_forecast"] = engine.category_forecast()
    forecast["income"] = income

    result = chatbot.get_response(message, forecast)
    return jsonify(result)
