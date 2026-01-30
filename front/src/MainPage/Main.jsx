import {useEffect, useRef, useState} from "react";
import {Router, useNavigate} from "react-router";
import ProfileAvatar from "../Profile/ProfileAvatar";
import '../styles/Main.css';
import { createStyles } from 'antd-style';

import CustomButton from "./CustomButton";
import {Route, Routes} from "react-router-dom";
import SubscriptionComponent from "./SubscriptionComponent";

const Main = () => {


    const [producerId, setProducerId] = useState();

    const navigate = useNavigate()

    useEffect(() => {


        const data = {

            'method' : 'GET',
            'credentials': 'include',
        }



        fetch("http://localhost:8080/auth_check", data)
            .then(response => {
                if(response.status === 401) {
                    fetch("http://localhost:8080/auth", {  'method' : 'GET',
                        'credentials': 'include',})
            }
        })
            }
        , [])





    const subscribeLink = () => {
        fetch("http://localhost:8081/get_currentId", {method: "GET", "credentials": "include"})
            .then(res => res.json())
            .then(data => {
                setProducerId(data.id)
                console.log(data)
                navigate(`/notification/${data.id}/subscribe`)
            })
    }

    const sendNotifications = () => {
        fetch("http://localhost:8081/get_currentId", {method: "GET", "credentials": "include"})
            .then(res => res.json())
            .then(data => {
                setProducerId(data.id)
                console.log(data)
                navigate(`/send/notification/${data.id}`)
            })
    }



    return (
        <div className={"mainPage"}>
            <div id="header">
                    <ProfileAvatar/>
            </div>

            <div className={"mainBody"}>
                <CustomButton onClick={subscribeLink} text={"Subscribe Link"} className={"mainButtons"}/>
                <CustomButton onClick={sendNotifications} text={"Send Notifications"} className={"mainButtons"}/>
            </div>

        </div>
    )



}


export default Main;