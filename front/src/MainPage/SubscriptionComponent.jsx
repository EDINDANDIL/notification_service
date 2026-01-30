import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import CustomButton from "./CustomButton";
import "../styles/Subscription.css";

const SubscriptionComponent = () => {
    const [denied, setDenied] = useState(Notification.permission === "denied");
    const [subscribed, setSubscribed] = useState(false);
    const [publicKey, setPublicKey] = useState("");
    const { id } = useParams();
    const [name, setName] = useState("");
    const navigate = useNavigate();
    const [registration, setRegistration] = useState(null);

    useEffect(() => {
        const registerSW = async () => {
            if ("serviceWorker" in navigator) {
                try {
                    const reg = await navigator.serviceWorker.register("/service-worker.js");
                    setRegistration(reg);
                    console.log("Service Worker зарегистрирован:", reg);

                    const sub = await reg.pushManager.getSubscription();
                    setSubscribed(!!sub);
                } catch (err) {
                    console.error("Ошибка регистрации Service Worker:", err);
                }
            }
        };

        registerSW();

        const fetchPublicKey = async () => {
            try {
                const res = await fetch("http://localhost:8081/get_key", {
                    method: "GET",
                    credentials: "include",
                });
                const data = await res.json();
                setPublicKey(data.key);
            } catch {
                console.error("Не удалось получить публичный ключ");
            }
        };

        fetchPublicKey();
    }, []);

    // Подписка на пуши
    const subscribe = async () => {
        const permission = await Notification.requestPermission();
        if (permission !== "granted") {
            setDenied(true);
            return;
        }

        if (!registration || !publicKey) {
            alert("Service Worker или ключ не загружены");
            return;
        }

        try {
            const subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(publicKey),
            });

            if (name.trim()) {
                await fetch(`http://localhost:8081/save-subscription/${id}?name=${name}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(subscription),
                    credentials: "include",
                });
            }

            setSubscribed(true);
            alert("Subscribed successfully!");
        } catch (err) {
            console.error("Ошибка подписки на пуши:", err);
        }
    };

    // Отписка
    const unsubscribe = async () => {
        if (!registration) return;

        const subscription = await registration.pushManager.getSubscription();
        if (subscription) {
            await subscription.unsubscribe();
            await fetch(`http://localhost:8081/unsubscribe`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ endpoint: subscription.endpoint }),
                credentials: "include",
            });
            setSubscribed(false);
            alert("Unsubscribed successfully!");
        }
    };

    function urlBase64ToUint8Array(base64String) {
        const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
        const raw = atob(base64);
        return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
    }

    return (
        <>
            <div className="header">
                <CustomButton onClick={() => navigate("/")} text="Home" />
            </div>
            <div className="subscriptionComponent">
                <h1>Web Push Notifications 📣</h1>
                {denied && (
                    <b>
                        You have blocked notifications. You need to manually enable them in your browser.
                    </b>
                )}

                {subscribed ? (
                    <div className="subscriptionContainer">
                        <CustomButton onClick={unsubscribe} text="Unsubscribe" className="subscribeButton" />
                    </div>
                ) : (
                    <div className="subscriptionContainer">
                        <input
                            placeholder="Type your name"
                            className="nameInput"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                        />
                        <CustomButton onClick={subscribe} text="Subscribe" className="subscribeButton" />
                    </div>
                )}
            </div>
        </>
    );
};

export default SubscriptionComponent;