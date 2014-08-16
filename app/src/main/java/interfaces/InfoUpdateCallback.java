package interfaces;

/**
 * Created by me on 7/31/14.
 */
public interface InfoUpdateCallback {
  public void onBitsCountUpdate(int count);

  public void onPostFailed(String json);
}
