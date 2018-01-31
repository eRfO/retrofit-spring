package com.mitobit.retrofit2.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;

import retrofit2.Converter;

/**
 * Enables exposure of {@link RetrofitService} instances.
 * 
 * @see #value()
 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(RetrofitBeanDefinitionRegistrar.class)
public @interface EnableRetrofitServices {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableRetrofitServices("org.my.pkg")} instead of {@code @EnableRetrofitServices(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};
	
	/**
	 * Configures the name of the {@link Converter.Factory} bean definition to be used to create services
	 * discovered through this annotation. Defaults to {@code jacksonConverterFactory}.
	 * 
	 */
	String converterFactoryRef() default RetrofitConstants.CONVERTER_FACTORY_BEAN_NAME;	
	
}
