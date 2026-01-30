import Icon, {GoogleOutlined} from "@ant-design/icons";

import React from "react";

import { Button } from 'antd';

const AuthButton = ({ children, onClick }) => {
    return (
        <>
            {React.Children.map(children, (item, index) => (
                <div key={index} className="auth-button">
                    <button key={index} onClick={() => onClick(item.type.displayName.toLowerCase().replace('outlined',''))}>
                        {item}
                    </button>
                </div>
            ))}
        </>
    );
};

export default AuthButton;

