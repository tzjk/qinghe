package com.qinghe.life;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QingheLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingheLifeApplication.class, args);
    }
}
