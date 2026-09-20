import React from "react";
import Predictions from "./components/Predictions";
import Chatbot from "./components/Chatbot";
import "./App.css";

// TODO: replace with the logged-in user once auth is wired in.
const currentUser = { id: 1 };

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Expense Tracker</h1>
      </header>

      <main className="app-main">
        <Predictions userId={currentUser.id} />
      </main>

      <Chatbot userId={currentUser.id} />
    </div>
  );
}
