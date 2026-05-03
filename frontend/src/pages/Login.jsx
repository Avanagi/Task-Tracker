import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

export default function Login({ setAuth }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
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
            alert('Ошибка входа. Проверьте логин и пароль.');
        }
    };

    return (
        <div className="card w-50 mx-auto mt-5">
            <div className="card-body">
                <h3 className="card-title">Вход в Task Tracker</h3>
                <form onSubmit={handleLogin}>
                    <input className="form-control mb-3" placeholder="Логин" onChange={e => setUsername(e.target.value)} required />
                    <input className="form-control mb-3" type="password" placeholder="Пароль" onChange={e => setPassword(e.target.value)} required />
                    <button className="btn btn-primary w-100" type="submit">Войти</button>
                </form>
                <div className="mt-3 text-center">
                    <Link to="/register">Нет аккаунта? Зарегистрируйтесь</Link>
                </div>
            </div>
        </div>
    );
}