package org.python279.jpmmlevaluatorservice;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class JpmmlEvaluatorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpmmlEvaluatorServiceApplication.class, args);
	}
}
