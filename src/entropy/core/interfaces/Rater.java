package entropy.core.interfaces;

public interface Rater {
  void delivered(Trop t, Trop ... children);
  void notDelivered(Trop t, Trop ... children);
  void blacklist(Trop t1, Trop t2);
}
