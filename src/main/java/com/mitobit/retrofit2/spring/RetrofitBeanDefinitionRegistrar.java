package com.mitobit.retrofit2.spring;


import static com.mitobit.retrofit2.spring.RetrofitConstants.CONVERTER_FACTORY_BEAN_NAME;
import static com.mitobit.retrofit2.spring.RetrofitConstants.CONVERTER_FACTORY_CLASS_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import retrofit2.Converter;

/**
 * A {@link ImportBeanDefinitionRegistrar} to allow annotation configuration of
 * Retrofit services scanning. Using an @Enable annotation allows beans to be
 * registered via @Component configuration, whereas implementing
 * {@code BeanDefinitionRegistryPostProcessor} will work for XML configuration.
 *
 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
 * 
 * @see RetrofitComponentProvider
 * @since 1.2.0
 */
public class RetrofitBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	private static final String HTTP_CLIENT_REF = "httpClient";
	private static final String CONVERTER_FACTORY_REF = "converterFactoryRef";

	private ResourceLoader resourceLoader;
	private Environment environment;	
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		Assert.notNull(importingClassMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");		
		
		registerConverterFactoryIfNecessary(registry);
		
		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableRetrofitServices.class.getName());
		String[] basePackages = (String[]) annotationAttributes.get("basePackages");
		String globalConverterFactoryRef = (String) annotationAttributes.get(CONVERTER_FACTORY_REF);
		if (basePackages == null || basePackages.length == 0) {
			basePackages = (String[]) annotationAttributes.get("value");
		}
		
		Iterable<? extends TypeFilter> includeFilters = new ArrayList<>();
		RetrofitComponentProvider scanner = new RetrofitComponentProvider(includeFilters);
		scanner.setResourceLoader(resourceLoader);
		scanner.setEnvironment(environment);

		for (TypeFilter filter : getExcludeFilters()) {
			scanner.addExcludeFilter(filter);
		}

		Set<BeanDefinition> result = new HashSet<BeanDefinition>();
		for (String basePackage : basePackages) {
			result.addAll(scanner.findCandidateComponents(basePackage));
		}

		for (BeanDefinition definition : result) {
			ScannedGenericBeanDefinition scanneDefinition = (ScannedGenericBeanDefinition) definition;
			String beanClassName = scanneDefinition.getBeanClassName();
			Map<String, Object> serviceAttributes = scanneDefinition.getMetadata().getAnnotationAttributes(RetrofitService.class.getName());
			Map<String, Object> qualifierAttributes = scanneDefinition.getMetadata().getAnnotationAttributes(Qualifier.class.getName());
			// check http client
			String httpClientRef = (String) serviceAttributes.get(HTTP_CLIENT_REF);
			// check converter factory
			String converterFactoryRef = (String) serviceAttributes.get(CONVERTER_FACTORY_REF);
			if (StringUtils.isEmpty(converterFactoryRef)) {
				converterFactoryRef = globalConverterFactoryRef;
			}



			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RetrofitServiceFactoryBean.class);
			builder.addPropertyValue("serviceInterface", beanClassName);
			builder.addPropertyValue("baseUrl", environment.resolvePlaceholders((String) serviceAttributes.get("baseUrl")));
			if (!StringUtils.isEmpty(httpClientRef)) {
				builder.addPropertyReference("httpClient", httpClientRef);
			}
			if (converterFactoryRef != null) {
				builder.addPropertyReference("converterFactory", converterFactoryRef);
			}
			
			AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
			// If the plugin interface has a Qualifier annotation, propagate that to the bean definition of the registry
			if (qualifierAttributes != null && !qualifierAttributes.isEmpty()) {
				AutowireCandidateQualifier qualifierMetadata = new AutowireCandidateQualifier(Qualifier.class);
				qualifierMetadata.setAttribute(AutowireCandidateQualifier.VALUE_KEY, qualifierAttributes.get("value"));
				beanDefinition.addQualifier(qualifierMetadata);
			}

			// Default
			registry.registerBeanDefinition(StringUtils.uncapitalize(beanClassName), builder.getBeanDefinition());			
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(org.springframework.core.env.Environment)
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns whether we should consider nested services, i.e. service interface definitions nested in other
	 * classes.
	 * 
	 * @return {@literal true} if the container should look for nested service interface definitions.
	 */
	public boolean shouldConsiderNestedServices() {
		return false;
	}		
	
	/**
	 * Return the {@link TypeFilter}s to define which types to exclude when scanning for repositories. Default
	 * implementation returns an empty collection.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected Iterable<TypeFilter> getExcludeFilters() {
		return Collections.emptySet();
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to include when scanning for repositories. Default
	 * implementation returns an empty collection.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected Iterable<TypeFilter> getIncludeFilters() {
		return Collections.emptySet();
	}

    /**
     * @param registry, the {@link BeanDefinitionRegistry} to be used to register the
     *          {@link Converter.Factory}.
     */
    private void registerConverterFactoryIfNecessary(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(CONVERTER_FACTORY_BEAN_NAME)) {
            return;
        }

        if (!ClassUtils.isPresent(CONVERTER_FACTORY_CLASS_NAME, getClass().getClassLoader())) {
            throw new BeanDefinitionStoreException(CONVERTER_FACTORY_CLASS_NAME + " not found. \n"
                    + "Could not configure Retrofit services feature because"
                    + " converter-jackson.jar is not on the classpath!\n"
                    + "If you want to use Retrofit please add converter-jackson.jar to the classpath.");
        }

        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClassName(CONVERTER_FACTORY_CLASS_NAME);
        def.setFactoryMethodName("create");
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        registry.registerBeanDefinition(CONVERTER_FACTORY_BEAN_NAME, new BeanComponentDefinition(def, CONVERTER_FACTORY_BEAN_NAME).getBeanDefinition());
    }	
	
}
