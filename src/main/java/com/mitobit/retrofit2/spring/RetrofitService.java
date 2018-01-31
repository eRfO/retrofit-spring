package com.mitobit.retrofit2.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Service;

import retrofit2.Converter;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Service
public @interface RetrofitService {
	
	String value() default "";
    
	String baseUrl();

	/**
	 * Specify new {@link okhttp3.OkHttpClient} bean name.
	 */
	String httpClient() default "";

	/**
	 * Configures the name of the {@link Converter.Factory} bean definition to be used to create services
	 * discovered through this annotation.
	 */
	String converterFactoryRef() default "";
	
}