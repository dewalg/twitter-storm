package com.rjil;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import java.lang.IllegalStateException;

public class cassClient {
   private Cluster cluster;
   private Session session;

   public Session getSession() {
      return this.session;
   }

   public void connect(String keyspace) {
      cluster = Cluster.builder()
            .addContactPoint("127.0.0.1")
            .build();
      Metadata metadata = cluster.getMetadata();
      System.out.println("Connected to cluster: " +
            metadata.getClusterName());
      for ( Host host : metadata.getAllHosts() ) {
         System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",
               host.getDatacenter(), host.getAddress(), host.getRack());
      }

      try {
          session = cluster.connect(keyspace);
      } catch (NoHostAvailableException e) {
          System.out.printf("No host available.");
          throw e;
      } catch (IllegalStateException e) {
          System.out.printf("illegally trying to connect.");
          throw e;
      }
   }

   public void close() {
      session.close();
      cluster.close();
   }

}
