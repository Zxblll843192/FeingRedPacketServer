package cn.zxblll.feingredpacketserver;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.zxblll.feingredpacketserver.mapper")
public class FeingRedPacketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeingRedPacketServerApplication.class, args);
    }

}
