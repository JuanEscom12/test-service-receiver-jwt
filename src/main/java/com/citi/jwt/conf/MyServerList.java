package com.citi.jwt.conf;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;

public class MyServerList extends AbstractServerList<Server> {
    private final DiscoveryClient discoveryClient;
    private IClientConfig clientConfig;

    public MyServerList(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public List<Server> getInitialListOfServers() {
        return getUpdatedListOfServers();
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        Server[] servers = discoveryClient.getInstances(clientConfig.getClientName()).stream()
            .map(i -> new Server(i.getHost(), i.getPort()))
            .toArray(Server[]::new);
        return Arrays.asList(servers);
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    } 

}
