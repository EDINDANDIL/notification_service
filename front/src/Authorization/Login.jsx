import React, {useEffect, useState} from 'react';
import '../styles/Login.css';
import {GithubOutlined, GoogleOutlined, LockOutlined, UserOutlined} from '@ant-design/icons';
import { Button, Checkbox, Form, Input, Flex } from 'antd';
import AuthButton from "./AuthButton";
import {useLocation, useNavigate} from "react-router";


const Login = () => {

    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(false);








    const onFinish = values => {
        const data = {
            headers: {
                'Content-Type': 'application/json'
            },
            'method' : 'POST',
            'credentials': 'include',
            body:  JSON.stringify(values)
        }

        fetch('http://localhost:8080/form-login', data)
            .then(res => res.json()).then((res) => {
            console.log(res);
        })
    };



    const handleRegisterButton = (provider) => {
        window.location.href = `http://localhost:8080/oauth2/authorization/${provider.toLowerCase()}`;
    };


    return (



        <div className="login">
        <div className="form-login">
        <Form
            name="login"
            initialValues={{ remember: true }}
            style={{ maxWidth: 360 }}
            onFinish={onFinish}
        >
            <Form.Item
                name="username"
                rules={[{ required: true, message: 'Please input your Username!' }]}
            >
                <Input prefix={<UserOutlined />} placeholder="Username" />
            </Form.Item>
            <Form.Item
                name="password"
                rules={[{ required: true, message: 'Please input your Password!' }]}
            >
                <Input prefix={<LockOutlined />} type="password" placeholder="Password" />
            </Form.Item>


            <Form.Item>
                <Button block type="primary" htmlType="submit">
                    Log in
                </Button>
                or <a href="/register">Register now!</a>
            </Form.Item>
        </Form>
        </div>
            Or sign in with
        <div className="buttons">
            <AuthButton onClick={(e) => handleRegisterButton(e)}>
                <GithubOutlined name = "GithubOutlined" />
            </AuthButton>
        </div>
        </div>


    );
};
export default Login;