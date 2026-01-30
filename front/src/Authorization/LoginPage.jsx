
import Login from "./Login";
import AuthButton from "./AuthButton";
import {GithubOutlined, GoogleOutlined} from "@ant-design/icons";

const LoginPage = () => {


    const handleRegister = (e) => {
        console.log(e)
    }



    return (
        <div className="login-page">

            <Login></Login>



        </div>
    )
}


export default LoginPage;