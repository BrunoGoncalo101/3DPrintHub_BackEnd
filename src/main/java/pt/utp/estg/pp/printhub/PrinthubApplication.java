package pt.utp.estg.pp.printhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrinthubApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrinthubApplication.class, args);
	}

}
