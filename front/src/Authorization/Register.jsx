import React, {useEffect} from 'react';
import {GithubOutlined, GoogleOutlined, LockOutlined, UserOutlined} from '@ant-design/icons';
import { Button,  Form, Input} from 'antd';
import AuthButton from "./AuthButton";
import {useNavigate} from "react-router";



const Register = () => {

    const navigate = useNavigate();



    useEffect(() => {
        fetch("http://localhost:8080/auth_check", {method: "GET", 'credentials': 'include'})
            .then(response => {
                if(response.ok) {
                navigate("/");
            }})
        })






    const onFinish = values => {
        const data = {
            headers: {
                'Content-Type': 'application/json'
            },
            'method' : 'POST',
            'credentials': 'include',
            body:  JSON.stringify(values)
        }

        fetch('http://localhost:8080/form-register', data)
            .then(res => {
                if(res.ok) {
                    navigate('/')
                }
            });
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
                            Register
                        </Button>
                        Already have an account? <a href="/login">Login now!</a>
                    </Form.Item>


                </Form>
            </div>


        </div>
    );
};
export default Register;