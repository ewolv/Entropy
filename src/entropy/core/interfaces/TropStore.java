package entropy.core.interfaces;

import java.util.Collection;

import entropy.core.exception.TropException;


public interface TropStore {
  
  void add(Collection<Trop> trops) throws TropException;
  
  Collection<Trop> getPayments();
  
  Collection<Trop> getDeliveries();
  
  Collection<Trop> getBlacklist();
  
  Collection<Trop> getTropById(String ... ids);
  
  void updateFiles(String... pathsToBeAdded);
  
  Collection<String> getFilePathsByDigest(Collection<String> digests);
  
  Collection<String> getTropIdsWithFiles();
}
