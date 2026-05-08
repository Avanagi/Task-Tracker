import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import './Tasks.css';
import './Auth.css';

export default function Register({ setAuth }) {
    const[formData, setFormData] = useState({ username: '', email: '', password: '' });
    const[globalError, setGlobalError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const[loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setGlobalError('');
        setFieldErrors({});
        setLoading(true);

        try {
            await axios.post('http://localhost:8080/api/auth/register', formData);

            const token = 'Basic ' + btoa(formData.username + ':' + formData.password);
            const response = await axios.get('http://localhost:8080/api/auth/me', {
                headers: { 'Authorization': token }
            });

            localStorage.setItem('auth', token);
            localStorage.setItem('username', formData.username);
            localStorage.setItem('userId', response.data.id);
            setAuth(token);
            navigate('/tasks');
        } catch (error) {
            console.error('Детали ошибки регистрации:', error);

            if (error.response) {
                const data = error.response.data;

                if (typeof data === 'object' && Object.keys(data).length > 0 && !data.message) {
                    setFieldErrors(data);
                } else if (data.message) {
                    setGlobalError(data.message);
                } else if (typeof data === 'string' && data.trim() !== '') {
                    setGlobalError(data);
                } else {
                    setGlobalError('Введены некорректные данные. Проверьте правильность заполнения.');
                }
            } else if (error.request) {
                setGlobalError('Сервер недоступен.');
            } else {
                setGlobalError('Произошла ошибка при отправке запроса.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="tasks-container auth-wrapper">
            <div className="form-card auth-card">
                <div className="text-center mb-4">
                    <h2 className="welcome-title">Регистрация</h2>
                    <p className="welcome-subtitle mt-2">Присоединяйтесь к нашей команде</p>
                </div>

                {globalError && <div className="auth-error-box">{globalError}</div>}

                <form onSubmit={handleRegister}>
                    <div className="auth-input-group">
                        <input
                            className={`form-input ${fieldErrors.username ? 'input-error' : ''}`}
                            placeholder="Логин"
                            onChange={e => setFormData({ ...formData, username: e.target.value })}
                            required
                        />
                        {fieldErrors.username && <div className="field-error-text">{fieldErrors.username}</div>}
                    </div>

                    <div className="auth-input-group">
                        <input
                            className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
                            type="email"
                            placeholder="Email"
                            onChange={e => setFormData({ ...formData, email: e.target.value })}
                            required
                        />
                        {fieldErrors.email && <div className="field-error-text">{fieldErrors.email}</div>}
                    </div>

                    <div className="auth-input-group-last">
                        <input
                            className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
                            type="password"
                            placeholder="Пароль"
                            onChange={e => setFormData({ ...formData, password: e.target.value })}
                            required
                        />

                        <div className="info-tooltip-container mt-2">
                            <span className="info-icon">ℹ️</span>
                            <span className="info-icon-label">Требования к полям</span>

                            <div className="tooltip-text tooltip-text-large">
                                <p className="tooltip-section-title">👤 Логин:</p>
                                <ul className="tooltip-list">
                                    <li>От 3 до 30 символов</li>
                                    <li>Только буквы (a-z), цифры и "_"</li>
                                </ul>

                                <p className="tooltip-section-title">📧 Почта:</p>
                                <ul className="tooltip-list">
                                    <li>Действующий формат (user@domain.com)</li>
                                </ul>

                                <p className="tooltip-section-title">🔒 Пароль:</p>
                                <ul className="tooltip-list">
                                    <li>Минимум 8 символов</li>
                                    <li>Хотя бы одна <strong>Заглавная</strong> буква</li>
                                    <li>Хотя бы одна <strong>цифра</strong></li>
                                </ul>
                            </div>
                        </div>
                        {fieldErrors.password && <div className="field-error-text">{fieldErrors.password}</div>}
                    </div>

                    <button className="submit-btn full-width-btn" type="submit" disabled={loading}>
                        {loading ? 'Создание аккаунта...' : 'Создать аккаунт'}
                    </button>
                </form>

                <div className="mt-4 text-center">
                    <Link to="/login" className="auth-link">
                        Уже есть аккаунт? Войти
                    </Link>
                </div>
            </div>
        </div>
    );
}