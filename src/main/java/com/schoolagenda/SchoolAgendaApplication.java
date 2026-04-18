package com.schoolagenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SchoolAgendaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchoolAgendaApplication.class, args);

	}

}
