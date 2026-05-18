package br.jus.tjpi.agendatelefonica;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgendaTelefonicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaTelefonicaApplication.class, args);
    }

}
