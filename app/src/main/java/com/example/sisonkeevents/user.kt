package com.example.sisonkeevents

class user {
    var Firstname: String?=null
    var LastName: String?=null
    var email:String?=null
    var PhoneNumber: String?=null
    var password: String?=null

    fun updateUserDetails(firstname: String, lastname: String, email: String, phonenumber: String, password: String){
        this.Firstname=firstname
        this.LastName=lastname
        this.email=email
        this.PhoneNumber=phonenumber
        this.password=password
    }
    fun verifyUser(email: String, password: String): Boolean{
        if(this.email==email && this.password==password){
            return true
        }else{
            return false
        }
    }
    fun verifyPasswords(confirmPassword: String,Password: String): Boolean{
        if(confirmPassword==Password){
            return true
        }else{
            return false
        }
    }
}

