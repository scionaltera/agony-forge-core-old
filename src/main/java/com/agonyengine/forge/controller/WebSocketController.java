package com.agonyengine.forge.controller;

import com.agonyengine.forge.controller.greeting.GreetingLoader;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.util.List;

@Controller
public class WebSocketController {
    private List<String> greeting;

    @Inject
    public WebSocketController(@Named("compositeGreetingLoader") GreetingLoader greetingLoader) {
        greeting = greetingLoader.load();
    }

    @Transactional
    @SubscribeMapping("/queue/output")
    public Output onSubscribe() {
        return new Output(greeting)
            .append("[yellow]Type something!")
            .append("[default]> ");
    }

    @Transactional
    @MessageMapping("/input")
    @SendToUser(value = "/queue/output", broadcast = false)
    public Output onInput(Input input) {
        return new Output("[cyan]" + input, "", "[default]> ");
    }
}
