package com.lwh.seckill.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/index")
public class TestController {

    @RequestMapping("/getIndex")
    public String getIndex(){
        return "newIndex";
    }
}
