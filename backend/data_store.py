"""
data_store.py — in-memory demo data for the expense tracker.

This stands in for the real database until it's wired to SQLAlchemy.
get_user_expenses() / get_user_income() are the two functions
routes.py calls — swap their bodies for real queries and everything
above them (predictions, chatbot) keeps working unchanged.

Seeded with one demo user (id=1) with expenses spread across 6 days,
per the integration guide's demo-day tip: the confidence score and
trend line need a handful of days of data to look right on stage.
"""

from datetime import date, timedelta

_today = date.today()

_DEMO_INCOME = {
    1: 15000.0,
}

_DEMO_EXPENSES = {
    1: [
        {"date": (_today - timedelta(days=6)).isoformat(), "amount": 450.0, "category": "Food"},
        {"date": (_today - timedelta(days=5)).isoformat(), "amount": 200.0, "category": "Transport"},
        {"date": (_today - timedelta(days=4)).isoformat(), "amount": 800.0, "category": "Food"},
        {"date": (_today - timedelta(days=3)).isoformat(), "amount": 1200.0, "category": "Shopping"},
        {"date": (_today - timedelta(days=2)).isoformat(), "amount": 300.0, "category": "Transport"},
        {"date": (_today - timedelta(days=1)).isoformat(), "amount": 650.0, "category": "Food"},
        {"date": _today.isoformat(), "amount": 150.0, "category": "Entertainment"},
    ],
}


def get_user_expenses(user_id):
    """Replace with a real SQLAlchemy query, e.g.:
    Expense.query.filter_by(user_id=user_id).all()
    """
    return _DEMO_EXPENSES.get(user_id, [])


def get_user_income(user_id):
    """Replace with a real SQLAlchemy query, e.g.:
    User.query.get(user_id).monthly_income
    """
    return _DEMO_INCOME.get(user_id, 0.0)
