package com.mitobit.retrofit2.spring;

import static org.springframework.util.Assert.notNull;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import retrofit2.Converter.Factory;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;

/**
 * BeanFactory that enables injection of RetrofitService interfaces. 
 * 
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
 * 
 */
public class RetrofitServiceFactoryBean<T> implements FactoryBean<T>, InitializingBean {

	private Class<T> serviceInterface;
	
	private OkHttpClient httpClient;

	private Factory converterFactory;
	
	private String baseUrl;
	
	/**
	 * Sets the service interface of the Retrofit adapter.
	 *
	 * @param serviceInterface
	 *            class of the interface
	 */
	public void setServiceInterface(Class<T> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Sets the {@link OkHttpClient} to add to Retrofit adapter.
	 *
	 * @param httpClient
	 */
	public void setHttpClient(OkHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Sets the converter factory to add to Retrofit adapter.
	 *
	 * @param converterFactory the converter factory instance
	 *           
	 */	
	public void setConverterFactory(Factory converterFactory) {
		this.converterFactory = converterFactory;
	}

	/**
	 * Set a fixed API base URL.
	 * 
	 * @param baseUrl
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public T getObject() throws Exception {
		Builder builder = new Retrofit.Builder().baseUrl(baseUrl);
		if (converterFactory != null) {
			builder.addConverterFactory(converterFactory);
		}
		if (httpClient != null) {
			builder.client(httpClient);
		}
		Retrofit retrofit = builder.build();
		return retrofit.create(serviceInterface);
	}

	@Override
	public Class<?> getObjectType() {
		return serviceInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		notNull(this.serviceInterface, "Property 'serviceInterface' is required.");
		notNull(this.baseUrl, "Property 'baseUrl' is required.");
	}

}
