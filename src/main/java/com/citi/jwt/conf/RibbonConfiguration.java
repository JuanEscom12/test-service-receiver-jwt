package com.citi.jwt.conf;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ServerList;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;

//@Configuration
public class RibbonConfiguration {

	@Autowired
	private DiscoveryClient discoveryClient;
	
	@Bean
	@ConditionalOnMissingBean
	public ServerList<?> ribbonServerList(IClientConfig config) {
		MyServerList myserverLis = new MyServerList(discoveryClient);
		myserverLis.initWithNiwsConfig(config);
		return myserverLis;
	}
	
	private AtomicReference<List<String>> catalogServicesState = new AtomicReference<>();
	
	@Autowired
	private ApplicationEventPublisher publisher;
	
	@Autowired
	private KubernetesClient kubernetesClient;
	
	@Scheduled(fixedDelayString = "${spring.cloud.kubernetes.discovery.catalogServicesWatchDelay:30000}")
	public void catalogServicesWatch() {
		try {
			List<String> previousState = this.catalogServicesState.get();

			// not all pods participate in the service discovery. only those that have
			// endpoints.
			List<Endpoints> endpoints = this.kubernetesClient.endpoints().list().getItems();
			List<String> endpointsPodNames = endpoints.stream().map(Endpoints::getSubsets).filter(Objects::nonNull)
					.flatMap(Collection::stream).map(EndpointSubset::getAddresses).filter(Objects::nonNull)
					.flatMap(Collection::stream).map(EndpointAddress::getTargetRef).filter(Objects::nonNull)
					.map(ObjectReference::getName) // pod name
													// unique in
													// namespace
					.sorted(String::compareTo).collect(Collectors.toList());

			this.catalogServicesState.set(endpointsPodNames);

			if (!endpointsPodNames.equals(previousState)) {
				
				this.publisher.publishEvent(new HeartbeatEvent(this, endpointsPodNames));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
