package org.gameontext.player;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;


@Readiness
@ApplicationScoped
public class PlayerHealth implements HealthCheck {

    @Inject
    protected CouchDbHealth dbHealth;

    @Override
    public HealthCheckResponse call() {
      if ( dbHealth.isHealthy() ) {
          return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                    .withData(dbHealth.getDatabaseName(), "available").up().build();
      }
      return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                .withData(dbHealth.getDatabaseName(), "down").down()
                                .build();
    }
}
