package com.heroku.internal;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Key;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;
import com.heroku.api.request.app.AppDestroy;
import com.heroku.api.request.key.KeyRemove;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    Main m = new Main();
    m.deleteApps();
    m.deleteKeys();
  }

  private String apiKey;

  private HerokuAPI api;

  Main() {
    this.apiKey = System.getenv("HEROKU_API_KEY");
    this.api = new HerokuAPI(apiKey);
  }

  void deleteKeys() throws InterruptedException {
    deleteKeys(api.listKeys());
  }

  void deleteKeys(List<Key> keysToDelete) throws InterruptedException {
    long start = System.currentTimeMillis();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (final Key res : keysToDelete) {
      executorService.execute(new Runnable() {
        public void run() {
          System.out.format("Deleting key %s\n", res.getComment());
          deleteKey(res.getId());
          System.out.format("Deleted key %s\n", res.getComment());
        }
      });
    }
    // await termination of all the threads to complete app deletion.
    executorService.shutdown();
    executorService.awaitTermination(300L, TimeUnit.SECONDS);
    System.out.format("Deleted keys in %dms\n", (System.currentTimeMillis() - start));
  }

  void deleteApps() throws InterruptedException {
    deleteApps(api.listApps());
  }

  void deleteApps(List<App> appsToDelete) throws InterruptedException {
    long start = System.currentTimeMillis();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (final App res : appsToDelete) {
      executorService.execute(new Runnable() {
        public void run() {
          System.out.format("Deleting app %s\n", res.getName());
          deleteApp(res.getName());
          System.out.format("Deleted app %s\n", res.getName());
        }
      });
    }
    // await termination of all the threads to complete app deletion.
    executorService.shutdown();
    executorService.awaitTermination(300L, TimeUnit.SECONDS);
    System.out.format("Deleted apps in %dms\n", (System.currentTimeMillis() - start));
  }

  void deleteKey(String sshkey) {
    try {
      api.getConnection().execute(new KeyRemove(sshkey), apiKey);
    } catch (RequestFailedException e) {
      if (e.getStatusCode() != Http.Status.FORBIDDEN.statusCode) {
        throw e;
      }
    }
  }

  void deleteApp(String appName) {
    try {
      api.getConnection().execute(new AppDestroy(appName), apiKey);
    } catch (RequestFailedException e) {
      if (e.getStatusCode() != Http.Status.FORBIDDEN.statusCode) {
        throw e;
      }
    }
  }
}
