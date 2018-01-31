package com.mitobit.retrofit2.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Custom {@link ClassPathScanningCandidateComponentProvider} scanning for interfaces extending the given base
 * interface.
 * 
 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
 */
class RetrofitComponentProvider extends ClassPathScanningCandidateComponentProvider {

	private static final String METHOD_NOT_PUBLIC = "AnnotationConfigUtils.processCommonDefinitionAnnotations(…) is not public! Make sure you're using Spring 3.2.5 or better. The class was loaded from %s.";

	private boolean considerNestedServiceInterfaces;

	/**
	 * Creates a new {@link RetrofitComponentProvider} using the given {@link TypeFilter} to include components to be
	 * picked up.
	 * 
	 * @param includeFilters the {@link TypeFilter}s to select service interfaces to consider, must not be
	 *          {@literal null}.
	 */
	public RetrofitComponentProvider(Iterable<? extends TypeFilter> includeFilters) {
		super(false);
		assertRequiredSpringVersionPresent();
		Assert.notNull(includeFilters, "Include filters cannot be null");
		if (includeFilters.iterator().hasNext()) {
			for (TypeFilter filter : includeFilters) {
				addIncludeFilter(filter);
			}
		} else {
			addIncludeFilter(null);
		}
	}

	/**
	 * Custom extension of {@link #addIncludeFilter(TypeFilter)} to extend the added {@link TypeFilter}. For the
	 * {@link TypeFilter} handed we'll have two filters registered: one additionally enforcing the
	 * {@link RetrofitService} annotation.
	 * 
	 * @see ClassPathScanningCandidateComponentProvider#addIncludeFilter(TypeFilter)
	 */
	@Override
	public void addIncludeFilter(TypeFilter includeFilter) {
		List<TypeFilter> filterPlusInterface = new ArrayList<TypeFilter>(2);
		filterPlusInterface.add(new AnnotationTypeFilter(RetrofitService.class));
		filterPlusInterface.add(new InterfaceTypeFilter());
		if (includeFilter != null) {
			filterPlusInterface.add(includeFilter);
		}
		super.addIncludeFilter(new AllTypeFilter(filterPlusInterface));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition)
	 */
	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		boolean isTopLevelType = !beanDefinition.getMetadata().hasEnclosingClass();
		boolean isConsiderNestedServices = isConsiderNestedServiceInterfaces();
		return (isTopLevelType || isConsiderNestedServices);
	}

	/**
	 * Customizes the service interface detection and triggers annotation detection on them.
	 */
	@Override
	public Set<BeanDefinition> findCandidateComponents(String basePackage) {
		Set<BeanDefinition> candidates = super.findCandidateComponents(basePackage);
		for (BeanDefinition candidate : candidates) {
			if (candidate instanceof AnnotatedBeanDefinition) {
				AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
			}
		}
		return candidates;
	}

	/**
	 * @return the considerNestedServiceInterfaces
	 */
	public boolean isConsiderNestedServiceInterfaces() {
		return considerNestedServiceInterfaces;
	}

	/**
	 * Controls whether nested inner-class {@link RetrofitService} interface definitions should be considered for automatic
	 * discovery. This defaults to {@literal false}.
	 * 
	 * @param considerNestedServiceInterfaces
	 */
	public void setConsiderNestedServiceInterfaces(boolean considerNestedServiceInterfaces) {
		this.considerNestedServiceInterfaces = considerNestedServiceInterfaces;
	}

	/**
	 * Makes sure {@link AnnotationConfigUtils#processCommonDefinitionAnnotations(AnnotatedBeanDefinition) is public and
	 * indicates the offending JAR if not.
	 */
	private static void assertRequiredSpringVersionPresent() {

		try {
			AnnotationConfigUtils.class.getMethod("processCommonDefinitionAnnotations", AnnotatedBeanDefinition.class);
		} catch (NoSuchMethodException o_O) {
			throw new IllegalStateException(String.format(METHOD_NOT_PUBLIC, AnnotationConfigUtils.class
					.getProtectionDomain().getCodeSource().getLocation()), o_O);
		}
	}

	/**
	 * {@link org.springframework.core.type.filter.TypeFilter} that only matches interfaces. Thus setting this up makes
	 * only sense providing an interface type as {@code targetType}.
	 * 
	 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
	 */
	private static class InterfaceTypeFilter implements TypeFilter {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.TypeFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
			return metadataReader.getClassMetadata().isInterface();
		}
	}

	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates match.
	 * 
	 * @author <a href="mailto:michele.blasi@mitobit.com">Michele Blasi</a>
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 * 
		 * @param delegates must not be {@literal null}.
		 */
		public AllTypeFilter(List<TypeFilter> delegates) {
			Assert.notNull(delegates, "delegates must not be null");
			this.delegates = delegates;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.TypeFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
			for (TypeFilter filter : delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}
			return true;
		}
	}
}
