import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function Register() {
    const[formData, setFormData] = useState({ username: '', email: '', password: '' });
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setError('');

        try {
            await axios.post('http://localhost:8080/api/auth/register', formData);
            alert('Регистрация успешна! Теперь войдите.');
            navigate('/login');
        } catch (error) {
            console.error('Registration error:', error.message);

            if (error.response) {
                const errorMessage = error.response.data?.message || error.response.data || 'Ошибка регистрации';
                alert(errorMessage);
                setError(errorMessage);
            } else if (error.request) {
                alert('Сервер не отвечает. Проверьте подключение.');
                setError('Сервер не отвечает');
            } else {
                alert('Ошибка: ' + error.message);
                setError(error.message);
            }
        }
    };

    return (
        <div className="card w-50 mx-auto mt-5">
            <div className="card-body">
                <h3 className="card-title">Регистрация</h3>
                {error && <div className="alert alert-danger">{error}</div>}
                <form onSubmit={handleRegister}>
                    <input
                        className="form-control mb-3"
                        placeholder="Логин"
                        onChange={e => setFormData({...formData, username: e.target.value})}
                        required
                    />
                    <input
                        className="form-control mb-3"
                        type="email"
                        placeholder="Email"
                        onChange={e => setFormData({...formData, email: e.target.value})}
                        required
                    />
                    <input
                        className="form-control mb-3"
                        type="password"
                        placeholder="Пароль"
                        onChange={e => setFormData({...formData, password: e.target.value})}
                        required
                    />
                    <button className="btn btn-success w-100" type="submit">Зарегистрироваться</button>
                </form>
            </div>
        </div>
    );
}