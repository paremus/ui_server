/*-
 * #%L
 * com.paremus.ui.rest.test
 * %%
 * Copyright (C) 2018 - 2019 Paremus Ltd
 * %%
 * Licensed under the Fair Source License, Version 0.9 (the "License");
 * 
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. You may not use this file 
 * except in compliance with the License. For usage restrictions see the 
 * LICENSE.txt file distributed with this work
 * #L%
 */
package com.paremus.ui.rest.test;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static org.apache.shiro.web.env.EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.DISPATCHER_ASYNC;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.DISPATCHER_ERROR;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.DISPATCHER_FORWARD;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.DISPATCHER_INCLUDE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.DISPATCHER_REQUEST;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN;
import static org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.env.MutableWebEnvironment;
import org.apache.shiro.web.env.WebEnvironment;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.annotations.RequireHttpWhiteboard;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;
import org.osgi.util.tracker.ServiceTracker;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

@RequireConfigurationAdmin
@RequireJaxrsWhiteboard
@RequireHttpWhiteboard
@Requirement(namespace=SERVICE_NAMESPACE, filter="(osgi.jaxrs.name=aries.shiro.authz)")
public class AbstractParemusRestUITestCase {

	private static final String CONTEXT_NAME = "paremus-root";
	private static final String CONTEXT_FILTER = format("(%s=%s)", HTTP_WHITEBOARD_CONTEXT_NAME, CONTEXT_NAME);

	private static final String VALID_USER = "USER";
	private static final String VALID_USER_CREDENTIAL = "PASS";

	protected final BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;
	private ServiceTracker<ClientBuilder, ClientBuilder> clientBuilderTracker;
	private ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime> runtimeTracker;

	private ServiceRegistration<ServletContextHelper> webContext;
	private ServiceRegistration<Filter> shiroFilter;

	protected SimpleAccountRealm realm;

	protected Client client;

	@Before
	public void setupWebLayer() throws Exception {
		cmTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);
		cmTracker.open();

		realm = setupRealm();
		webContext = registerContext();
		shiroFilter = registerShiroFilter();

		ConfigurationAdmin cm = cmTracker.waitForService(3000);

		assertNotNull(cm);

		configureJAXRSWhiteboard(cm);
		configureShiroAuthz(cm);
		configureJackson(cm);

		clientBuilderTracker = new ServiceTracker<>(
	            context, ClientBuilder.class, null);
	    clientBuilderTracker.open();

	    runtimeTracker = new ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime>(
	    		context, JaxrsServiceRuntime.class, null) {
	    	public JaxrsServiceRuntime addingService(ServiceReference<JaxrsServiceRuntime> ref) {
	    		try {
					return context.createFilter("(paremus.whiteboard.name=paremus-rest)").match(ref) ? super.addingService(ref) : null;
				} catch (InvalidSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
	    	}
	    };
	    runtimeTracker.open();

	    client = getClientBuilder().register(JacksonJaxbJsonProvider.class).build();
	}

	private SimpleAccountRealm setupRealm() {
		SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();

		simpleAccountRealm.addAccount(VALID_USER, VALID_USER_CREDENTIAL);
		return simpleAccountRealm;
	}

	private ServiceRegistration<ServletContextHelper> registerContext() {
		ServletContextHelper helper = getServletContextHelper();

		Dictionary<String, Object> props = getServletContextProps();

		return context.registerService(ServletContextHelper.class, helper, props);
	}

	protected ServletContextHelper getServletContextHelper() {
		ServletContextHelper helper = new ServletContextHelper() {

			@Override
			public URL getResource(String name) {
				if("login.jsp".equals(name)) {
					return AbstractParemusRestUITestCase.class.getResource("/login.jsp");
				}
				return super.getResource(name);
			}
		};
		return helper;
	}

	protected Dictionary<String, Object> getServletContextProps() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HTTP_WHITEBOARD_CONTEXT_NAME, CONTEXT_NAME);
		props.put(HTTP_WHITEBOARD_CONTEXT_PATH, "/ui");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/login.jsp");
		return props;
	}

	private ServiceRegistration<Filter> registerShiroFilter() {
		ShiroFilter filter = new ShiroFilter() {
			public void init() throws Exception {

				WebEnvironment env = getShiroWebEnvironment(filterConfig);

				getServletContext().setAttribute(ENVIRONMENT_ATTRIBUTE_KEY, env);

				super.init();
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(HTTP_WHITEBOARD_CONTEXT_SELECT, CONTEXT_FILTER);
		props.put(HTTP_WHITEBOARD_FILTER_NAME, "ShiroAuthenticationFilter");
		props.put(HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		props.put(HTTP_WHITEBOARD_FILTER_DISPATCHER, new String[] { DISPATCHER_ASYNC, DISPATCHER_ERROR,
				DISPATCHER_FORWARD, DISPATCHER_INCLUDE, DISPATCHER_REQUEST });

		return context.registerService(Filter.class, filter, props);
	}

	private void configureJAXRSWhiteboard(ConfigurationAdmin cm) throws IOException {
		Configuration config = cm.getFactoryConfiguration("org.apache.aries.jax.rs.whiteboard", "test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HTTP_WHITEBOARD_CONTEXT_SELECT, CONTEXT_FILTER);
		props.put("paremus.whiteboard.name", "paremus-rest");
		config.update(props);
	}

	private void configureShiroAuthz(ConfigurationAdmin cm) throws IOException {
		Configuration config = cm.getConfiguration("org.apache.aries.jax.rs.shiro.authorization", "?");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=paremus-rest-ui)");
		props.put("paremus.security.authz", "shiro");
		config.update(props);
	}

	private void configureJackson(ConfigurationAdmin cm) throws IOException {
		Configuration config = cm.getConfiguration("org.apache.aries.jax.rs.jackson", "?");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(JAX_RS_APPLICATION_SELECT, "(osgi.jaxrs.name=paremus-rest-ui)");
		config.update(props);
	}

	@After
	public void tearDownWebLayer() throws Exception {
		webContext.unregister();
		shiroFilter.unregister();

		ConfigurationAdmin cm = cmTracker.waitForService(3000);

		assertNotNull(cm);

		cm.getFactoryConfiguration("org.apache.aries.jax.rs.whiteboard", "test").delete();
		cm.getConfiguration("org.apache.aries.jax.rs.shiro.authorization", "?").delete();
		cm.getConfiguration("org.apache.aries.jax.rs.jackson", "?").delete();

		cmTracker.close();
		clientBuilderTracker.close();
		runtimeTracker.close();
	}

	protected ClientBuilder getClientBuilder() {
		try {
			return clientBuilderTracker.waitForService(3000);
		} catch (InterruptedException e) {
			Assert.fail("Interrupted while waiting for client builder");
			return null;
		}
	}

	protected JaxrsServiceRuntime getRuntime() {
		try {
			return runtimeTracker.waitForService(3000);
		} catch (InterruptedException e) {
			Assert.fail("Interrupted while waiting for JAX-RS runtime");
			return null;
		}
	}

	protected String getBaseURI() {
		Object property = runtimeTracker.getServiceReference().getProperty(JAX_RS_SERVICE_ENDPOINT);
		if(property instanceof String) {
			return property.toString();
		} else if (property instanceof Collection) {
			return ((Collection<?>)property).iterator().next().toString();
		} else if (property instanceof String[]) {
			return ((String[]) property)[0];
		}
		throw new IllegalArgumentException("The base URI was " + property);
	}

	protected WebEnvironment getShiroWebEnvironment(FilterConfig config) {
		MutableWebEnvironment env = new DefaultWebEnvironment();
		env.setWebSecurityManager(new DefaultWebSecurityManager(realm));

		PathMatchingFilterChainResolver filterChainResolver = new PathMatchingFilterChainResolver(config);
		filterChainResolver.getFilterChainManager().createChain("/login.jsp", "authc");
		filterChainResolver.getFilterChainManager().createChain("/logout", "logout");

		env.setFilterChainResolver(filterChainResolver);

		return env;
	}

	protected Map<String, NewCookie> login(Client client) {
		Form form = new Form();
		form.param("username", VALID_USER);
		form.param("password", VALID_USER_CREDENTIAL);

		Response response = client.target(getBaseURI()).path("ui").path("login.jsp").request().post(entity(form, APPLICATION_FORM_URLENCODED_TYPE));

		assertTrue("The login failed with status " + response.getStatusInfo(), response.getStatusInfo().getFamily().compareTo(Status.Family.REDIRECTION) <= 0);

		return response.getCookies();
	}

}
