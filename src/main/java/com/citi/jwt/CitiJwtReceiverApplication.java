package com.citi.jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.citi.jwt.conf.RibbonConfiguration;



@SpringBootApplication
@EnableAutoConfiguration( exclude = RabbitAutoConfiguration.class) 
@ComponentScan("com.citi")
@EnableDiscoveryClient
@EnableScheduling
@AutoConfigureAfter(RibbonAutoConfiguration.class)
@RibbonClients(defaultConfiguration = RibbonConfiguration.class) 
public class CitiJwtReceiverApplication {
		
	public static void main(String[] args) {
		SpringApplication.run(CitiJwtReceiverApplication.class, args);
	}
	
}