import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Tasks from './pages/Tasks.jsx';

export default function App() {
    const [auth, setAuth] = useState(localStorage.getItem('auth'));

    return (
        <Router>
            <div className="container mt-4">
                <Routes>
                    <Route path="/" element={<Navigate to="/login" />} />

                    <Route path="/login" element={auth ? <Navigate to="/tasks" /> : <Login setAuth={setAuth} />} />
                    <Route path="/register" element={auth ? <Navigate to="/tasks" /> : <Register setAuth={setAuth} />} />

                    <Route path="/tasks" element={auth ? <Tasks auth={auth} setAuth={setAuth} /> : <Navigate to="/login" />} />
                </Routes>
            </div>
        </Router>
    );
}