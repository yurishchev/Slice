/*-
 * #%L
 * Slice - Core
 * %%
 * Copyright (C) 2012 Cognifide Limited
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.cognifide.slice.core.internal.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cognifide.slice.api.annotation.OsgiService;

public class BundleClassesFinder {

	private static final Logger LOG = LoggerFactory.getLogger(BundleClassesFinder.class);

	private static final String RESOURCE_PATTERN = "*.class";

	private final Collection<Bundle> bundles;

	private final String basePackage;

	private List<ClassFilter> filters = new ArrayList<BundleClassesFinder.ClassFilter>();

	public BundleClassesFinder(String basePackage, String bundleNameFilter, BundleContext bundleContext) {
		this.bundles = findBundles(bundleNameFilter, bundleContext);
		this.basePackage = basePackage.replace('.', '/');
	}

	public Collection<Class<?>> getClasses() {
		Collection<Class<?>> classes = new ArrayList<Class<?>>();
		for (Bundle bundle : this.bundles) {
			@SuppressWarnings("unchecked")
			Enumeration<URL> classEntries = bundle.findEntries(this.basePackage, RESOURCE_PATTERN, true);
			while ((classEntries != null) && classEntries.hasMoreElements()) {
				try {
					URL classURL = classEntries.nextElement();
					ClassReader classReader = new ClassReader(classURL.openStream());
					if (accepts(classReader)) {
						String className = classReader.getClassName().replace('/', '.');
						if (LOG.isDebugEnabled()) {
							LOG.debug("Class: " + className + " has been found.");
						}
						Class<?> clazz = bundle.loadClass(className);
						classes.add(clazz);
					}
				} catch (ClassNotFoundException e) {
					LOG.error("Error loading class!", e);
				} catch (IOException e) {
					LOG.error("Error reading the class!", e);
				}
			}
		}
		return classes;
	}

	private boolean accepts(ClassReader classReader) {
		for (ClassFilter classFilter : this.filters) {
			if (classFilter.accepts(classReader)) {
				return true;
			}
		}
		return false;
	}

	public void addFilter(ClassFilter classFilter) {
		this.filters.add(classFilter);
	}

	public interface ClassFilter {
		boolean accepts(ClassReader classReader);
	}

	public Collection<Class<?>> traverseBundlesForOsgiServices() {
		Collection<Class<?>> allClasses = getClasses();
		Set<Class<?>> osgiClasses = new HashSet<Class<?>>();
		for (Class<?> clazz : allClasses) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(OsgiService.class)) {
					Class<?> fieldClass = field.getType();
					osgiClasses.add(fieldClass);
				}
			}

			Constructor<?>[] constructors = clazz.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				Annotation[][] annotations = constructor.getParameterAnnotations();
				for (int i = 0; i < parameterTypes.length; i++) {
					for (Annotation annotation : annotations[i]) {
						if (annotation.annotationType().equals(OsgiService.class)) {
							osgiClasses.add(parameterTypes[i]);
						}
						break;
					}
				}
			}
		}
		return osgiClasses;
	}

	List<Bundle> findBundles(String bundleNameFilter, BundleContext bundleContext) {
		Pattern bundleNamePattern = Pattern.compile(bundleNameFilter);
		List<Bundle> bundles = new ArrayList<Bundle>();
		for (Bundle bundle : bundleContext.getBundles()) {
			if (bundleNamePattern.matcher(bundle.getSymbolicName()).matches()) {
				bundles.add(bundle);
			}
		}
		return bundles;
	}
}
