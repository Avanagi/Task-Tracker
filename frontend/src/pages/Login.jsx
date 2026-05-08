import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import './Tasks.css';
import './Auth.css';

export default function Login({ setAuth }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        const token = 'Basic ' + btoa(username + ':' + password);
        try {
            const response = await axios.get('http://localhost:8080/api/auth/me', {
                headers: { 'Authorization': token }
            });
            localStorage.setItem('auth', token);
            localStorage.setItem('username', username);
            localStorage.setItem('userId', response.data.id);
            setAuth(token);
            navigate('/tasks');
        } catch (error) {
            setError('Неверный логин или пароль. Попробуйте снова.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="tasks-container auth-wrapper">
            <div className="form-card auth-card">
                <div className="text-center mb-4">
                    <h2 className="welcome-title"><span className="emoji">🚀</span> Task Tracker</h2>
                    <p className="welcome-subtitle mt-2">С возвращением! Войдите в аккаунт.</p>
                </div>

                {error && <div className="auth-error-box">{error}</div>}

                <form onSubmit={handleLogin}>
                    <div className="login-input-wrapper">
                        <input className="form-input" placeholder="Логин" value={username} onChange={e => setUsername(e.target.value)} required />
                    </div>
                    <div className="login-password-wrapper">
                        <input className="form-input" type="password" placeholder="Пароль" value={password} onChange={e => setPassword(e.target.value)} required />
                    </div>
                    <button className="submit-btn full-width" type="submit" disabled={loading}>
                        {loading ? 'Вход...' : 'Войти'}
                    </button>
                </form>

                <div className="mt-4 text-center">
                    <Link to="/register" className="auth-link">Нет аккаунта? Зарегистрируйтесь</Link>
                </div>
            </div>
        </div>
    );
}