package com.mattymatty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class RestApi {
    static ServerController controller;
    public static void main(String[] arguments){
        new Thread((controller = new ServerController(23446,"rolegroup"))).start();
        SpringApplication.run(RestApi.class,arguments);

        Runtime.getRuntime().addShutdownHook(new Thread(ServerController::close));
    }

}
