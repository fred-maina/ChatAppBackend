package com.fredmaina.chatapp.Controllers;


import com.fredmaina.chatapp.DTOs.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class ChatController {

    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public Message send(Message message) throws Exception {
        return message;
    }
}
