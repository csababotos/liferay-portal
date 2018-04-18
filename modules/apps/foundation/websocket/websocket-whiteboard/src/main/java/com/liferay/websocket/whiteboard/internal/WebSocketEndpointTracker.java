/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.websocket.whiteboard.internal;

import com.liferay.portal.kernel.util.StringBundler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContext;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Cristina González
 * @author Manuel de la Peña
 */
@Component(immediate = true)
public class WebSocketEndpointTracker
	implements ServiceTrackerCustomizer<Endpoint, ServerEndpointConfigWrapper> {

	@Override
	public ServerEndpointConfigWrapper addingService(
		ServiceReference<Endpoint> serviceReference) {

		String path = (String)serviceReference.getProperty(
			"org.osgi.http.websocket.endpoint.path");

		if ((path == null) || path.isEmpty()) {
			return null;
		}

		Object decodersObject = serviceReference.getProperty(
			"org.osgi.http.websocket.endpoint.decoders");
		Object encodersObject = serviceReference.getProperty(
			"org.osgi.http.websocket.endpoint.encoders");
		Object subprotocolObject = serviceReference.getProperty(
			"org.osgi.http.websocket.endpoint.subprotocol");

		List<Class<? extends Decoder>> decoders = _decodersObjectToList(
			decodersObject);
		List<Class<? extends Encoder>> encoders = _encodersObjectToList(
			encodersObject);
		List<String> subprotocol = _subprotocolObjectToList(subprotocolObject);

		final ServiceObjects<Endpoint> serviceObjects =
			_bundleContext.getServiceObjects(serviceReference);

		ServerEndpointConfigWrapper serverEndpointConfigWrapper =
			_serverEndpointConfigWrappers.get(path);

		boolean isNew = false;

		if (serverEndpointConfigWrapper == null) {
			serverEndpointConfigWrapper = new ServerEndpointConfigWrapper(
				path, decoders, encoders, subprotocol, _logService);

			isNew = true;
		}
		else {
			Class<?> endpointClass =
				serverEndpointConfigWrapper.getEndpointClass();

			ServerEndpointConfig.Configurator configurator =
				serverEndpointConfigWrapper.getConfigurator();

			try {
				Object endpointInstance = configurator.getEndpointInstance(
					endpointClass);

				Class<?> endpointInstanceClass = endpointInstance.getClass();

				if (endpointInstanceClass.equals(
						ServerEndpointConfigWrapper.NullEndpoint.class)) {

					serverEndpointConfigWrapper.override(
						decoders, encoders, subprotocol);
				}
			}
			catch (InstantiationException ie) {
				Endpoint endpoint = serviceObjects.getService();

				_logService.log(
					LogService.LOG_ERROR,
					StringBundler.concat(
						"Unable to register WebSocket endpoint ",
						String.valueOf(endpoint.getClass()), " for path ",
						path),
					ie);
			}
		}

		serverEndpointConfigWrapper.setConfigurator(
			serviceReference,
			new ServiceObjectsConfigurator(serviceObjects, _logService));

		if (isNew) {
			ServerContainer serverContainer =
				(ServerContainer)_servletContext.getAttribute(
					ServerContainer.class.getName());

			try {
				serverContainer.addEndpoint(serverEndpointConfigWrapper);
			}
			catch (DeploymentException de) {
				Endpoint endpoint = serviceObjects.getService();

				_logService.log(
					LogService.LOG_ERROR,
					StringBundler.concat(
						"Unable to register WebSocket endpoint ",
						String.valueOf(endpoint.getClass()), " for path ",
						path),
					de);

				return null;
			}

			_serverEndpointConfigWrappers.put(
				path, serverEndpointConfigWrapper);
		}

		return serverEndpointConfigWrapper;
	}

	@Override
	public void modifiedService(
		ServiceReference<Endpoint> serviceReference,
		ServerEndpointConfigWrapper serverEndpointConfigWrapper) {

		removedService(serviceReference, serverEndpointConfigWrapper);

		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<Endpoint> serviceReference,
		ServerEndpointConfigWrapper serverEndpointConfigWrapper) {

		ServiceObjectsConfigurator serviceObjectsConfigurator =
			serverEndpointConfigWrapper.removeConfigurator(serviceReference);

		serviceObjectsConfigurator.close();
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		_serverEndpointConfigWrapperServiceTracker = new ServiceTracker<>(
			bundleContext, Endpoint.class, this);

		_serverEndpointConfigWrapperServiceTracker.open();
	}

	@Deactivate
	protected void deactivate() {
		_serverEndpointConfigWrapperServiceTracker.close();
	}

	@SuppressWarnings("unchecked")
	private List<Class<? extends Decoder>> _decodersObjectToList(
		Object decObject) {

		if (decObject == null) {
			return null;
		}

		if (decObject instanceof List) {
			return (List<Class<? extends Decoder>>)decObject;
		}

		String[] stringArray = _propObjectToStringArray(decObject);

		List<Class<? extends Decoder>> decoders = new ArrayList<>();

		for (String className : stringArray) {
			try {
				Class<? extends Decoder> c = (Class<? extends Decoder>) Class.forName(className);

				decoders.add(c);
				_logService.log(
					LogService.LOG_INFO, c.getCanonicalName() + " added...");
			}
			catch (ClassNotFoundException cnfe) {
				_logService.log(LogService.LOG_WARNING, cnfe.getMessage());
			}
			catch (Exception exc) {
				_logService.log(LogService.LOG_WARNING, exc.getMessage());
			}
		}

		return decoders;
	}

	@SuppressWarnings("unchecked")
	private List<Class<? extends Encoder>> _encodersObjectToList(
		Object encObject) {

		if (encObject == null) {
			return null;
		}

		if (encObject instanceof List) {
			return (List<Class<? extends Encoder>>)encObject;
		}

		String[] stringArray = _propObjectToStringArray(encObject);

		List<Class<? extends Encoder>> encoders = new ArrayList<>();

		for (String className : stringArray) {
			try {
				Class<? extends Encoder> c = (Class<? extends Encoder>) Class.forName(className);

				encoders.add(c);
				_logService.log(
					LogService.LOG_INFO, c.getCanonicalName() + " added...");
			}
			catch (ClassNotFoundException cnfe) {
				_logService.log(LogService.LOG_WARNING, cnfe.getMessage());
			}
			catch (Exception exc) {
				_logService.log(LogService.LOG_WARNING, exc.getMessage());
			}
		}

		return encoders;
	}

	@SuppressWarnings("unchecked")
	private List<String> _subprotocolObjectToList(Object spObject) {
		if (spObject == null) {
			return null;
		}

		if (spObject instanceof List) {
			return (List<String>)spObject;
		}

		String[] stringArray = _propObjectToStringArray(spObject);

		List<String> subprotocol = new ArrayList<>();

		for (String s : stringArray) {
			subprotocol.add(s);
		}

		return subprotocol;
	}

	private String[] _propObjectToStringArray(Object prop) {
		String[] stringArray = null;

		if (prop instanceof String) {
			stringArray = new String[1];

			stringArray[0] = (String)prop;
		}
		else {
			stringArray = (String[])prop;
		}

		return stringArray;
	}

	private BundleContext _bundleContext;

	@Reference
	private LogService _logService;

	private final ConcurrentMap<String, ServerEndpointConfigWrapper>
		_serverEndpointConfigWrappers = new ConcurrentHashMap<>();
	private ServiceTracker<Endpoint, ServerEndpointConfigWrapper>
		_serverEndpointConfigWrapperServiceTracker;

	@Reference(target = "(websocket.active=true)")
	private ServletContext _servletContext;

}