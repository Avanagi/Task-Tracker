import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import Select from 'react-select';
import './Tasks.css';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const glassSelectStyles = {
    menuPortal: base => ({ ...base, zIndex: 9999 }),
    control: base => ({
        ...base,
        background: 'rgba(0, 0, 0, 0.2)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        color: 'white',
        boxShadow: 'none',
        minHeight: '42px'
    }),
    singleValue: base => ({ ...base, color: 'white' }),
    multiValue: base => ({ ...base, backgroundColor: 'rgba(255,255,255,0.15)', borderRadius: '4px' }),
    multiValueLabel: base => ({ ...base, color: 'white' }),
    multiValueRemove: base => ({ ...base, color: 'white', ':hover': { backgroundColor: 'rgba(220, 53, 69, 0.5)' } }),
    menu: base => ({
        ...base,
        background: '#0f172a',
        border: '1px solid rgba(255, 255, 255, 0.1)'
    }),
    option: (base, state) => ({
        ...base,
        backgroundColor: state.isFocused ? 'rgba(255,255,255,0.1)' : 'transparent',
        color: 'white',
        cursor: 'pointer'
    })
};

export default function Tasks({ auth, setAuth }) {
    const [tasks, setTasks] = useState([]);
    const [users, setUsers] = useState([]);
    const [showForm, setShowForm] = useState(false);
    const [selectedTask, setSelectedTask] = useState(null);
    const[loading, setLoading] = useState(false);
    const [filter, setFilter] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');

    const username = localStorage.getItem('username');
    const userId = localStorage.getItem('userId');
    const navigate = useNavigate();

    const [newTask, setNewTask] = useState({
        type: 'TASK',
        title: '',
        description: '',
        dueDate: '',
        stepsToReproduce: '',
        subtaskTitles: '',
        assigneeIds:[],
        reporterId: userId
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const [tRes, uRes] = await Promise.all([
                axios.get('http://localhost:8080/api/tasks', { headers: { 'Authorization': auth } }),
                axios.get('http://localhost:8080/api/users', { headers: { 'Authorization': auth } })
            ]);
            setTasks(tRes.data);
            setUsers(uRes.data);
        } catch (err) {
            console.error(err);
            if (err.response?.status === 401) {
                localStorage.clear();
                setAuth(null);
                navigate('/login');
            }
        } finally {
            setLoading(false);
        }
    };

    const changeDeadline = async (taskId, newDate) => {
        if (!newDate) return;
        const formattedDate = newDate.length === 16 ? newDate + ':00' : newDate;
        try {
            await axios.patch(`http://localhost:8080/api/tasks/${taskId}/deadline?dueDate=${formattedDate}`, null, {
                headers: { 'Authorization': auth }
            });
            fetchData(false);
            setSelectedTask(prev => prev ? { ...prev, dueDate: formattedDate } : null);
        } catch (err) {
            console.error('Ошибка:', err);
            alert('Не удалось изменить дедлайн');
        }
    };

    useEffect(() => {
        fetchData(true);

        const socket = new SockJS('http://localhost:8080/ws');
        const stompClient = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                stompClient.subscribe('/topic/tasks', (message) => {
                    fetchData(false);
                });
            }
        });
        stompClient.activate();

        return () => {
            stompClient.deactivate();
        };
    }, [auth]);

    useEffect(() => {
        const timeoutIds =[];

        tasks.forEach(task => {
            if (task.dueDate && task.status !== 'DONE') {
                const dueTime = new Date(task.dueDate).getTime();
                const timeToDeadline = dueTime - Date.now();

                const MAX_TIMEOUT = 2147483647;

                if (timeToDeadline > 0 && timeToDeadline <= MAX_TIMEOUT) {
                    const timeoutId = setTimeout(() => {
                        console.log(`Дедлайн задачи #${task.id} наступил!`);
                        setTasks(prevTasks => [...prevTasks]);
                    }, timeToDeadline);

                    timeoutIds.push(timeoutId);
                }
            }
        });

        return () => {
            timeoutIds.forEach(id => clearTimeout(id));
        };
    }, [tasks]);

    const toggleSubtask = async (taskId, subtaskId) => {
        try {
            await axios.patch(`http://localhost:8080/api/tasks/${taskId}/subtasks/${subtaskId}/toggle`, null, { headers: { 'Authorization': auth } });
            setSelectedTask(prev => ({
                ...prev, subtasks: prev.subtasks.map(s => s.id === subtaskId ? { ...s, completed: !s.completed } : s)
            }));
            fetchData();
        } catch (err) {
            if (err.response?.status === 403) {
                alert('У вас нет прав! Отмечать чек-лист может только создатель или исполнитель задачи.');
            } else {
                console.error('Ошибка при обновлении подзадачи:', err);
            }
        }
    };

    const handleCreateTask = async (e) => {
        e.preventDefault();
        setLoading(true);
        const payload = { ...newTask };

        if (payload.type !== 'BUG') delete payload.stepsToReproduce;
        if (payload.type === 'EPIC') payload.subtaskTitles = payload.subtaskTitles ? payload.subtaskTitles.split(',').map(s => s.trim()) :[];
        else delete payload.subtaskTitles;
        if (payload.dueDate.length === 16) payload.dueDate += ':00';

        try {
            await axios.post('http://localhost:8080/api/tasks', payload, { headers: { 'Authorization': auth } });
            setShowForm(false);
            setNewTask({
                type: 'TASK', title: '', description: '', dueDate: '',
                stepsToReproduce: '', subtaskTitles: '', assigneeIds:[], reporterId: userId
            });
            fetchData();
        } catch (err) {
            console.error('Ошибка при создании задачи:', err);
            alert('Не удалось создать задачу. Подробности в консоли.');
        } finally {
            setLoading(false);
        }
    };

    const changeStatus = async (id, newStatus) => {
        try {
            await axios.patch(`http://localhost:8080/api/tasks/${id}/status?status=${newStatus}`, null, { headers: { 'Authorization': auth } });
            fetchData();
        } catch (err) {
            if (err.response?.status === 403) {
                alert('У вас нет прав! Только автор или исполнитель могут менять статус.');
            } else {
                console.error('Ошибка:', err);
            }
            fetchData();
        }
    };

    const updateAssignees = async (taskId, userIds) => {
        try {
            const idsParam = userIds.length > 0 ? userIds.join(',') : '';
            await axios.patch(`http://localhost:8080/api/tasks/${taskId}/assign-bulk?userIds=${idsParam}`, null, { headers: { 'Authorization': auth } });
            fetchData();
        } catch (err) {
            if (err.response?.status === 403) {
                alert('У вас нет прав! Только автор задачи может изменять исполнителей.');
            } else {
                console.error('Ошибка:', err);
            }
            fetchData();
        }
    };

    const deleteTask = async (id) => {
        if (window.confirm('Вы уверены, что хотите удалить эту задачу? Это действие нельзя отменить.')) {
            setLoading(true);
            try {
                await axios.delete(`http://localhost:8080/api/tasks/${id}`, { headers: { 'Authorization': auth } });
                fetchData();
                setSelectedTask(null);
            } catch (err) {
                console.error('Ошибка при удалении задачи:', err);
                alert('Не удалось удалить задачу');
            } finally {
                setLoading(false);
            }
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'TODO': return 'warning';
            case 'IN_PROGRESS': return 'info';
            case 'REVIEW': return 'warning-review';
            case 'DONE': return 'success';
            default: return 'secondary';
        }
    };

    const getTypeIcon = (type) => {
        switch (type) {
            case 'TASK': return '📋';
            case 'BUG': return '🐛';
            case 'EPIC': return '⭐';
            default: return '📌';
        }
    };

    const isOverdue = (task) => {
        if (!task.dueDate || task.status === 'DONE') return false;
        return new Date(task.dueDate) < new Date();
    };

    const filteredTasks = tasks.filter(task => {
        if (filter !== 'ALL' && task.type !== filter) return false;
        if (searchTerm && !task.title.toLowerCase().includes(searchTerm.toLowerCase())) return false;
        return true;
    });

    const userOptions = users.map(u => ({ value: u.id, label: u.username }));

    return (
        <div className="tasks-container">
            <div className="header-card">
                <div className="header-content">
                    <div className="welcome-section">
                        <h1 className="welcome-title"><span className="emoji">📊</span> Task Manager</h1>
                        <p className="welcome-subtitle">Добро пожаловать, <strong>{username}</strong></p>
                    </div>
                    <button className="logout-btn" onClick={() => { localStorage.clear(); setAuth(null); navigate('/login'); }}>
                        <span className="emoji">🚪</span> Выйти
                    </button>
                </div>
            </div>

            <div className="actions-bar">
                <button className="create-task-btn" onClick={() => setShowForm(!showForm)}>
                    <span className="emoji">{showForm ? '✖️' : '➕'}</span>
                    {showForm ? 'Скрыть форму' : 'Создать задачу'}
                </button>

                <div className="filters">
                    <div className="search-box">
                        <input type="text" className="search-input" placeholder="🔍 Поиск задач..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
                    </div>
                    <div className="filter-buttons" >
                        <button className={`filter-btn ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>Все</button>
                        <button className={`filter-btn ${filter === 'TASK' ? 'active' : ''}`} onClick={() => setFilter('TASK')}>📋 Задачи</button>
                        <button className={`filter-btn ${filter === 'BUG' ? 'active' : ''}`} onClick={() => setFilter('BUG')}>🐛 Баги</button>
                        <button className={`filter-btn ${filter === 'EPIC' ? 'active' : ''}`} onClick={() => setFilter('EPIC')}>⭐ Эпики</button>
                    </div>
                </div>
            </div>

            {showForm && (
                <div className="form-card slide-down">
                    <h3 className="form-title">✨ Новая задача</h3>
                    <form onSubmit={handleCreateTask}>
                        <div style={{ marginBottom: '15px' }}>
                            <label className="form-label">Тип задачи</label>
                            <select className="form-select" onChange={e => setNewTask({...newTask, type: e.target.value})} value={newTask.type}>
                                <option value="TASK">📋 Обычная задача</option>
                                <option value="BUG">🐛 Баг</option>
                                <option value="EPIC">⭐ Эпик</option>
                            </select>
                        </div>

                        <div className="row" style={{ display: 'flex', gap: '15px', marginBottom: '15px' }}>
                            <div className="col" style={{ flex: 1 }}>
                                <label className="form-label">Название *</label>
                                <input className="form-input" required onChange={e => setNewTask({...newTask, title: e.target.value})} />
                            </div>
                            <div className="col" style={{ flex: 1 }}>
                                <label className="form-label">Дедлайн *</label>
                                <input type="datetime-local" className="form-input" required onChange={e => setNewTask({...newTask, dueDate: e.target.value})} />
                            </div>
                        </div>

                        <div style={{ marginBottom: '15px' }}>
                            <label className="form-label">Описание</label>
                            <textarea className="form-textarea" rows="3" onChange={e => setNewTask({...newTask, description: e.target.value})} />
                        </div>

                        <div style={{ marginBottom: '15px' }}>
                            <label className="form-label">👥 Исполнители</label>
                            <Select
                                isMulti
                                options={userOptions}
                                placeholder="Выберите исполнителей..."
                                noOptionsMessage={() => "Сотрудники не найдены"}
                                styles={glassSelectStyles}
                                menuPortalTarget={document.body}
                                onChange={(selectedOptions) => {
                                    const ids = selectedOptions ? selectedOptions.map(opt => opt.value) :[];
                                    setNewTask({ ...newTask, assigneeIds: ids });
                                }}
                            />
                        </div>

                        {newTask.type === 'BUG' && (
                            <div style={{ marginBottom: '15px' }}>
                                <label className="form-label">🔍 Шаги воспроизведения (для бага)</label>
                                <textarea className="form-textarea" rows="2" onChange={e => setNewTask({...newTask, stepsToReproduce: e.target.value})} style={{ border: '1px solid #dc3545' }}/>
                            </div>
                        )}
                        {newTask.type === 'EPIC' && (
                            <div style={{ marginBottom: '15px' }}>
                                <label className="form-label">✅ Чек-лист (Текст подзадач, через запятую)</label>
                                <input className="form-input" onChange={e => setNewTask({...newTask, subtaskTitles: e.target.value})} style={{ border: '1px solid #0d6efd' }}/>
                            </div>
                        )}

                        <button type="submit" className="submit-btn" disabled={loading}>
                            {loading ? 'Создание...' : '✅ Создать задачу'}
                        </button>
                    </form>
                </div>
            )}

            {loading && (<div className="loading-spinner"><div className="spinner"></div><p>Выполняется запрос...</p></div>)}

            {!loading && (
                <div className="tasks-table-container">
                    <table className="tasks-table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Тип</th>
                            <th>Данные задачи</th>
                            <th>Исполнители</th>
                            <th>Статус</th>
                            <th>Действия</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredTasks.length === 0 ? (
                            <tr>
                                <td colSpan="6">
                                    <div className="empty-state">
                                        <div className="empty-emoji">📭</div>
                                        <p>Задачи не найдены</p>
                                    </div>
                                </td>
                            </tr>
                        ) : (
                            filteredTasks.map(t => {
                                const currentAssignees = t.assignees?.map(a => ({ value: a.id, label: a.username })) ||[];
                                const overdue = isOverdue(t);

                                const isReporter = t.reporter?.id === userId;
                                const lockedForMe = overdue && !isReporter;

                                return (
                                    <tr key={t.id} className={`task-row ${overdue ? 'overdue-row' : ''}`}>
                                        <td data-label="ID"><span className="task-id">#{t.id}</span></td>
                                        <td data-label="Тип">
                                            <span className="task-type" title={t.type}>
                                                {getTypeIcon(t.type)} {t.type}
                                            </span>
                                        </td>
                                        <td data-label="Название">
                                            <strong>{t.title}</strong>
                                            {overdue && <span className="overdue-badge">🔥 ПРОСРОЧЕНО</span>}
                                            {lockedForMe && <span className="badge bg-secondary ms-2" style={{fontSize: '10px'}}>🔒 ЗАБЛОКИРОВАНО</span>}
                                            <br/>
                                            <small className="task-description" style={{color: '#6c757d', marginTop: '5px'}}>
                                                👤 Автор: <strong style={{color: '#fff'}}>{t.reporter?.username || 'Неизвестен'}</strong> <br/>
                                                📆 Дедлайн: <span style={{ color: overdue ? '#fca5a5' : 'inherit' }}>
                                                    {t.dueDate ? new Date(t.dueDate).toLocaleString() : 'Нет'}
                                                </span>
                                            </small>
                                        </td>
                                        <td data-label="Исполнители" style={{ minWidth: '250px' }}>
                                            <Select
                                                isMulti
                                                options={userOptions}
                                                value={currentAssignees}
                                                placeholder="Не назначено"
                                                menuPortalTarget={document.body}
                                                styles={glassSelectStyles}
                                                isDisabled={!isReporter}
                                                onChange={(selectedOptions) => {
                                                    const ids = selectedOptions ? selectedOptions.map(opt => opt.value) : [];
                                                    updateAssignees(t.id, ids);
                                                }}
                                            />
                                        </td>
                                        <td data-label="Статус">
                                            <select
                                                className={`status-select status-${getStatusColor(t.status)}`}
                                                value={t.status}
                                                onChange={(e) => changeStatus(t.id, e.target.value)}
                                            >
                                                <option value="TODO">📝 TODO</option>
                                                <option value="IN_PROGRESS">⚙️ IN PROGRESS</option>
                                                <option value="REVIEW">⚙️ REVIEW</option>
                                                <option value="DONE">✅ DONE</option>
                                            </select>
                                        </td>
                                        <td data-label="Действия">
                                            <div className="action-buttons">
                                                <button className="action-btn view-btn" onClick={() => setSelectedTask(t)}>👁️</button>
                                                <button className="action-btn delete-btn" onClick={() => deleteTask(t.id)}>🗑️</button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                        </tbody>
                    </table>
                </div>
            )}

            {selectedTask && (
                <div className="modal-overlay" onClick={() => setSelectedTask(null)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>{getTypeIcon(selectedTask.type)} {selectedTask.title}</h3>
                            <button className="modal-close" onClick={() => setSelectedTask(null)}>✖️</button>
                        </div>
                        <div className="modal-body">
                            <p><strong>Описание:</strong> {selectedTask.description || 'Нет описания'}</p>

                            {selectedTask.type === 'BUG' && (
                                <p className="text-danger"><strong>Шаги воспроизведения:</strong> {selectedTask.stepsToReproduce}</p>
                            )}

                            {selectedTask.type === 'EPIC' && selectedTask.subtasks && (
                                <div>
                                    <strong>Чек-лист:</strong>
                                    <ul className="subtask-list">
                                        {selectedTask.subtasks.map(s => (
                                            <li key={s.id} className="subtask-item">
                                                <input
                                                    type="checkbox"
                                                    checked={s.completed}
                                                    onChange={() => toggleSubtask(selectedTask.id, s.id)}
                                                    className="me-2"
                                                />
                                                <span className={s.completed ? "completed" : ""}>{s.title}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setSelectedTask(null)}>Закрыть</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}