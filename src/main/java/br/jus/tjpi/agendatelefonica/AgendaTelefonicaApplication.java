package br.jus.tjpi.agendatelefonica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgendaTelefonicaApplication {

	public static void main(String[] args) {
		PostgresDockerBootstrap.startIfNeeded();
		SpringApplication.run(AgendaTelefonicaApplication.class, args);
	}

}
