package com.lcj.campusreco;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lcj.campusreco.mapper")
public class CampusRecoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRecoApplication.class, args);
    }
}
