package com.chess.chessverse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String,Object> health(){
        Map<String,Object> res = new HashMap<>();
        res.put("status","UP");
        res.put("ok",true);
        return res;
    }
}
