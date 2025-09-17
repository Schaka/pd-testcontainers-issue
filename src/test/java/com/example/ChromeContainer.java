package com.example;

import com.github.dockerjava.api.model.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class ChromeContainer extends GenericContainer<ChromeContainer> {

    private static final int PORT = 9222;

    private String cdpAddress = null;

    public ChromeContainer( final String dockerImageName ) {
        super( dockerImageName );
        this
                .withCreateContainerCmdModifier( it -> it.withHostConfig( HostConfig.newHostConfig( )
                        .withCapAdd( Capability.SYS_ADMIN )
                        .withPortBindings( new PortBinding( Ports.Binding.empty( ), ExposedPort.tcp( PORT ) ) ) ) )
                .withAccessToHost( true )
                .withExposedPorts( PORT )
                .waitingFor( Wait.forHttp( "/" ) );
        setStartupAttempts( 3 );
    }

    public ChromeContainer( final DockerImageName dockerImageName ) {
        this( dockerImageName.asCanonicalNameString( ) );
    }

    @Override
    public void start( ) {
        super.start( );
    }

    public String getCDPAddress( ) throws URISyntaxException, IOException {
        if ( cdpAddress == null ) {
            final URL url = new URI( "http", String.format( "%s:%s", getHost( ), getFirstMappedPort( ) ), "/json/version", null, null ).toURL( );
            final ObjectMapper mapper = new ObjectMapper( );
            cdpAddress = mapper.readTree( url ).get( "webSocketDebuggerUrl" ).textValue( );
        }

        return cdpAddress;
    }

    @Override
    public boolean equals( final Object o ) {
        return this == o;
    }

    @Override
    public int hashCode( ) {
        return System.identityHashCode( this );
    }

}