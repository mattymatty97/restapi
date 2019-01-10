package com.mattymatty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


@SpringBootApplication
public class RestApi extends SpringBootServletInitializer {
    static ServerController controller;
    public static void main(String[] arguments){
        new Thread((controller = new ServerController(23446,"rolegroup"))).start();
        SpringApplication.run(RestApi.class,arguments);

        Runtime.getRuntime().addShutdownHook(new Thread(ServerController::close));
    }

}
