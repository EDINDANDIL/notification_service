import React, { useState } from "react";
import Papa from "papaparse";
import "../styles/NotificationForm.css";
import {useParams} from "react-router";

const NotificationForm = () => {
    const [names, setNames] = useState([]); // без пустого элемента
    const [message, setMessage] = useState("");
    const [error, setError] = useState(false);
    const { id } = useParams();


    const handleFileUpload = (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        Papa.parse(file, {
            complete: (results) => {
                const parsedNames = results.data
                    .flat()
                    .map((name) => String(name).trim())
                    .filter((name) => name.length > 0);

                setNames((prev) => [...prev, ...parsedNames]);
            },
        });
    };

    const handleAddName = () => setNames([...names, ""]);

    const handleNameChange = (index, value) => {
        const newNames = [...names];
        if (value.trim().length === 0) {
            newNames.splice(index, 1);
        } else {
            newNames[index] = value;
        }
        setNames(newNames);
    };

    const handleSubmit = () => {
        const filteredNames = names.filter((n) => n.trim().length > 0);
        if(filteredNames.length === 0 || message === ""){
            setError(true);
            return;
        }

        fetch(`http://localhost:8081/notificate?id=${id}`, {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ names: filteredNames, message: message }),
        }).then((res) => res.json())
            .then((data) => {console.log(data.status)})
            .catch(error => {
                fetch(`http://localhost:8080/auth`, {method: "GET", credentials: "include"})
                    .then(fetch(`http://localhost:8081/notificate?id=${id}`, {
                        method: "POST",
                        credentials: "include",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ names: filteredNames, message: message }),
                    }).then((res) => res.json())
                        .then((data) => {console.log(data.status)}))
            });
        setError(false)
    };

    return (
        <div className="notification-form">
            <h2>Web-Push рассылка</h2>

            <div className="form-grid">
                <div className="names-column">
                    <label>Имена получателей</label>
                    {names.map((name, index) => (
                        <input
                            key={index}
                            type="text"
                            placeholder="Введите имя"
                            value={name}
                            onChange={(e) => handleNameChange(index, e.target.value)}
                        />
                    ))}
                    <button type="button" onClick={handleAddName}>
                        + Добавить имя
                    </button>
                </div>

                <div className="csv-column">
                    <label>Загрузить CSV с именами</label>
                    <input type="file" accept=".csv" onChange={handleFileUpload} />
                </div>
            </div>

            <div className="message-box">
                <label>Текст уведомления</label>
                <input
                    type="text"
                    placeholder="Введите сообщение..."
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                />
            </div>

            <button onClick={handleSubmit} className="send-btn">
                Отправить уведомление
            </button>

            {
                error ? (<p className={"errorText"}>Enter data, please</p>) : (null)
            }
        </div>
    );
};

export default NotificationForm;